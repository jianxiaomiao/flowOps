"""Seed and verify Nexus intake using a local, disposable FlowOps MySQL 8.4 schema."""

import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shlex
import subprocess
import sys
import threading
from datetime import datetime, timezone
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener
from uuid import uuid4


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def mysql(sql):
    completed = subprocess.run(
        ["mysql", f"--login-path={os.environ['FLOWOPS_ACCEPTANCE_MYSQL_LOGIN_PATH']}",
         "--host=127.0.0.1", "--protocol=TCP", f"--port={os.environ['FLOWOPS_ACCEPTANCE_MYSQL_PORT']}",
         f"--database={os.environ['FLOWOPS_ACCEPTANCE_SCHEMA']}", "--batch", "--skip-column-names"],
        input=sql, text=True, capture_output=True, check=True,
    )
    return completed.stdout.strip()


def bcrypt(password):
    value = subprocess.run(["htpasswd", "-niB", "-C", "10", "fixture"], input=password + "\n",
                           capture_output=True, text=True, check=True).stdout.strip().split(":", 1)[1]
    check(bool(re.fullmatch(r"\$2[aby]\$10\$[./A-Za-z0-9]{53}", value)), "Expected a BCrypt hash")
    return value


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def seed(suffix):
    a, b = f"nexus_a_{suffix}", f"nexus_b_{suffix}"
    password = secrets.token_urlsafe(24)
    machine_password = secrets.token_urlsafe(24)
    human_hash, machine_hash = bcrypt(password), bcrypt(machine_password)
    credentials = []
    sql = ["START TRANSACTION;"]
    for tenant in (a, b):
        secret = secrets.token_urlsafe(32)
        digest = hashlib.sha256(secret.encode("ascii")).hexdigest()
        credentials.append(f"{tenant}.{secret}")
        sql.append(f"""
        INSERT INTO tenant (tenant_code, tenant_name, status, created_at, updated_at)
            VALUES ('{tenant}', '{tenant}', 'ACTIVE', NOW(3), NOW(3));
        SET @tenant = LAST_INSERT_ID();
        INSERT INTO department (tenant_id, department_code, department_name, status, created_at, updated_at)
            VALUES (@tenant, 'support', 'Support', 'ACTIVE', NOW(3), NOW(3));
        SET @support = LAST_INSERT_ID();
        INSERT INTO department (tenant_id, department_code, department_name, status, created_at, updated_at)
            VALUES (@tenant, 'other', 'Other', 'ACTIVE', NOW(3), NOW(3));
        SET @other = LAST_INSERT_ID();
        INSERT INTO sla_policy (tenant_id, policy_name, priority, response_minutes, resolution_minutes,
            enabled, created_at, updated_at) VALUES (@tenant, 'Nexus P2', 'P2', 60, 240, TRUE, NOW(3), NOW(3));
        INSERT INTO user_account (tenant_id, department_id, username, password_hash, display_name,
            account_kind, status, created_at, updated_at)
        VALUES (@tenant, @support, 'nexus', '{machine_hash}', 'Nexus', 'MACHINE', 'ACTIVE', NOW(3), NOW(3));
        SET @actor = LAST_INSERT_ID();
        INSERT INTO ticket_integration (id, tenant_id, department_id, actor_id, priority)
            VALUES ('{tenant}', @tenant, @support, @actor, 'P2');
        INSERT INTO ticket_integration_key (key_id, integration_id, secret_digest)
            VALUES ('{tenant}', '{tenant}', UNHEX('{digest}'));
        INSERT INTO role (tenant_id, role_code, role_name, status, created_at, updated_at)
            VALUES (@tenant, 'SUPPORT', 'Support', 'ACTIVE', NOW(3), NOW(3));
        SET @role = LAST_INSERT_ID();
        INSERT INTO role_permission (tenant_id, role_id, permission_id, granted_at)
            SELECT @tenant, @role, id, NOW(3) FROM permission WHERE permission_code IN
                ('ticket:claim', 'ticket:read:department', 'ticket:create', 'ticket:assign', 'ticket:accept');
        INSERT INTO role (tenant_id, role_code, role_name, status, created_at, updated_at)
            VALUES (@tenant, 'READ_ONLY', 'Read only', 'ACTIVE', NOW(3), NOW(3));
        SET @read_role = LAST_INSERT_ID();
        INSERT INTO role_permission (tenant_id, role_id, permission_id, granted_at)
            SELECT @tenant, @read_role, id, NOW(3) FROM permission WHERE permission_code = 'ticket:read:department';
        INSERT INTO user_account (tenant_id, department_id, username, password_hash, display_name,
            status, created_at, updated_at)
        VALUES (@tenant, @support, 'staff1', '{human_hash}', 'Staff 1', 'ACTIVE', NOW(3), NOW(3)),
               (@tenant, @support, 'staff2', '{human_hash}', 'Staff 2', 'ACTIVE', NOW(3), NOW(3)),
               (@tenant, @other, 'staff_other', '{human_hash}', 'Other Staff', 'ACTIVE', NOW(3), NOW(3)),
               (@tenant, @support, 'staff_reader', '{human_hash}', 'Read Only', 'ACTIVE', NOW(3), NOW(3));
        INSERT INTO user_role (tenant_id, user_id, role_id, assigned_at)
            SELECT @tenant, id, IF(username = 'staff_reader', @read_role, @role), NOW(3)
            FROM user_account WHERE tenant_id = @tenant AND account_kind = 'HUMAN';
        SELECT @tenant, @support;
        """)
    sql.append("COMMIT;")
    routes = [tuple(map(int, row.split("\t"))) for row in mysql("\n".join(sql)).splitlines()]
    return (a, b), routes, credentials, password, machine_password


def api(base, path, token=None, data=None):
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = Request(base + path, data=None if data is None else json.dumps(data).encode(), headers=headers)
    try:
        with build_opener(ProxyHandler({}), NoRedirect()).open(req, timeout=15) as response:
            body = response.read()
            return response.status, json.loads(body) if body else None
    except HTTPError as error:
        with error:
            return error.code, None


def expect(response, status):
    actual, data = response
    check(actual == status, f"Expected HTTP {status}, got {actual}")
    return data


def race(operation, values):
    gate = threading.Barrier(2, timeout=5)

    def run(value):
        gate.wait()
        return operation(value)

    with concurrent.futures.ThreadPoolExecutor(2) as workers:
        return list(workers.map(run, values))


def main():
    base = os.environ["FLOWOPS_ACCEPTANCE_URL"].rstrip("/")
    url = urlsplit(base)
    check(url.scheme == "http" and url.hostname == "127.0.0.1" and url.port and url.path == "/flowOps"
          and not (url.username or url.query or url.fragment), "Use http://127.0.0.1:<port>/flowOps")
    check(bool(re.fullmatch(r"flowops_test_[a-z0-9_]+", os.environ["FLOWOPS_ACCEPTANCE_SCHEMA"])),
          "Use a disposable flowops_test_<name> schema")
    local_env = Path(os.environ["FLOWOPS_ACCEPTANCE_ENV_FILE"])
    check(local_env.parent.is_dir() and not local_env.exists(), "Use a new local file for the credential exports")
    server = mysql("SELECT VERSION();")
    check(server.startswith("8.4."), "Expected MySQL 8.4")
    check(mysql("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '5' AND success = TRUE;") == "1",
          "FlowOps Flyway V5 must have completed")
    print(f"PASS MySQL {server}, Flyway V5")
    (a, b), routes, machines, password, machine_password = seed(secrets.token_hex(5))

    def login(tenant, user, pw=password, status=200):
        reply = expect(api(base, "/auth/login", data={"tenantCode": tenant, "username": user, "password": pw}), status)
        return reply["data"]["accessToken"] if reply else None

    login(a, "nexus", machine_password, 401)
    staff1, staff2, other, reader = (login(a, user) for user in ("staff1", "staff2", "staff_other", "staff_reader"))
    staff_b = login(b, "staff1")
    ingest = "/integration/v1/tickets"
    event = {"schemaVersion": 1, "eventId": str(uuid4()), "externalTicketId": str(uuid4()),
             "title": "Nexus acceptance", "description": "Disposable FlowOps acceptance ticket",
             "submittedAt": datetime.now(timezone.utc).isoformat()}
    ack = expect(api(base, ingest, machines[0], event), 201)
    check(expect(api(base, ingest, machines[0], event), 200) == ack, "Replay changed the receipt")
    expect(api(base, ingest, machines[0], {**event, "title": "Changed"}), 409)
    for field in ("tenantId", "departmentId", "handlingDepartmentId", "actorId"):
        expect(api(base, ingest, machines[0], {**event, field: 999}), 400)
    expect(api(base, ingest + "?tenantId=999", machines[0], event), 400)
    expect(api(base, ingest, staff1, event), 401)
    ack_b = expect(api(base, ingest, machines[1], event), 201)
    linked_ids = [int(receipt["flowOpsTicketId"]) for receipt in (ack, ack_b)]
    check(linked_ids[0] != linked_ids[1], "Bindings shared a ticket")
    for identifier, receipt, token, route in zip(linked_ids, (ack, ack_b), (staff1, staff_b), routes):
        row = expect(api(base, f"/ticket/{identifier}", token), 200)["data"]
        check((row["tenantId"], row["handlingDepartmentId"]) == route, "Unexpected database route")
        check(row["ticketNo"] == receipt["ticketNo"] and row["status"] == "ASSIGNED"
              and row["currentAssigneeId"] is None and row["priority"] == "P2"
              and row["responseDueAt"] and row["resolutionDueAt"], "Unexpected new ticket state")
        print(f"PASS route tenant={route[0]}, department={route[1]}, id={identifier}, no={row['ticketNo']}")
    print("PASS sequential import 201/200/409, same receipt, rejected routing fields 400, staff JWT 401")

    fresh = {**event, "eventId": str(uuid4()), "externalTicketId": str(uuid4())}
    first, second = race(lambda _: api(base, ingest, machines[0], fresh), (1, 2))
    check(sorted((first[0], second[0])) == [200, 201] and first[1] == second[1], "Concurrent import duplicated")
    duplicate_id = int(first[1]["flowOpsTicketId"])
    check(mysql(f"SELECT COUNT(*) FROM external_ticket_link WHERE ticket_id = {duplicate_id};") == "1",
          "Unexpected number of import links")
    print("PASS concurrent import 200/201, one link")

    ticket_id = linked_ids[0]
    ticket = f"/ticket/{ticket_id}"
    version = {"version": expect(api(base, ticket, staff1), 200)["data"]["version"]}
    for token, status in ((other, 404), (staff_b, 404), (reader, 403)):
        expect(api(base, ticket + "/claim", token, version), status)
    print("PASS other department 404, other tenant 404, missing claim permission 403")
    results = race(lambda token: api(base, ticket + "/claim", token, version), (staff1, staff2))
    check(sorted(result[0] for result in results) == [200, 409], "Expected one successful claim")
    winner = (staff1, staff2)[next(i for i, result in enumerate(results) if result[0] == 200)]
    for table, column in (("ticket_transition", "action"), ("ticket_assignment", "assignment_type")):
        check(mysql(f"SELECT COUNT(*) FROM {table} WHERE ticket_id = {ticket_id} AND {column} = 'CLAIM';") == "1",
              f"Unexpected CLAIM count in {table}")
    queue = "/ticket/external-queue?pageSize=50"
    available = expect(api(base, queue, winner), 200)["data"]["records"]
    owned = expect(api(base, queue + "&mine=true", winner), 200)["data"]["records"]
    check(not any(row["id"] == ticket_id for row in available), "Ticket still available to claim")
    check(any(row["id"] == ticket_id and row["status"] == "ACCEPTED" for row in owned), "Winner cannot see ticket")
    print(f"PASS claim 200/409, id={ticket_id}, one CLAIM transition and one assignment, moved into winner's queue")

    department = routes[0][1]
    original = expect(api(base, "/ticket", staff1, {"title": "Legacy accept", "description": "Acceptance fixture",
                       "handlingDepartmentId": department, "priority": "P2", "ticketType": "INCIDENT"}), 200)["data"]
    original_path = f"/ticket/{original['id']}"
    assignee = expect(api(base, "/auth/me", staff2), 200)["data"]["userId"]
    assigned = expect(api(base, original_path + "/assignments", staff1,
                          {"toDepartmentId": department, "toAssigneeId": assignee, "version": original["version"]}), 200)["data"]
    expect(api(base, original_path + "/claim", staff2, {"version": assigned["version"]}), 404)
    accepted = expect(api(base, original_path + "/transitions", staff2,
                          {"action": "ACCEPT", "version": assigned["version"]}), 200)["data"]
    check(accepted["status"] == "ACCEPTED" and accepted["currentAssigneeId"] == assignee, "Legacy ACCEPT failed")
    print("PASS existing named assignment and ACCEPT")

    unclaim_path = f"/ticket/{duplicate_id}"
    to_department = {"toDepartmentId": department}
    unclaimed = expect(api(base, unclaim_path, staff1), 200)["data"]
    machine_user = int(mysql(f"SELECT actor_id FROM ticket_integration WHERE id = '{a}';"))
    expect(api(base, unclaim_path + "/assignments", staff1,
               {**to_department, "toAssigneeId": machine_user, "version": unclaimed["version"]}), 409)
    assigned = expect(api(base, unclaim_path + "/assignments", staff1,
                          {**to_department, "toAssigneeId": assignee, "version": unclaimed["version"]}), 200)["data"]
    expect(api(base, unclaim_path + "/assignments", staff1,
               {**to_department, "version": assigned["version"]}), 200)
    check(mysql(f"SELECT current_assignee_id IS NULL FROM ticket WHERE id = {duplicate_id};") == "1",
          "Returning ticket to department did not clear the assignee")
    reopened = expect(api(base, queue, staff1), 200)["data"]["records"]
    check(any(row["id"] == duplicate_id for row in reopened), "Returned ticket missing from available queue")
    print("PASS machine assignee rejected, department reassignment cleared assignee and restored queue visibility")

    exports = {"NEXUS_SUPPORT_FLOWOPS_INTEGRATION_ID": a, "NEXUS_SUPPORT_FLOWOPS_ENDPOINT": base + ingest,
               "NEXUS_SUPPORT_FLOWOPS_CREDENTIAL": machines[0], "FLOWOPS_E2E_API_BASE_URL": base,
               "FLOWOPS_E2E_TENANT_CODE": a, "FLOWOPS_E2E_USERNAME": "staff1", "FLOWOPS_E2E_PASSWORD": password,
               "FLOWOPS_E2E_MACHINE_CREDENTIAL": machines[0], "NEXUS_FLOWOPS_TEST_FLOWOPS_URL": base,
               "NEXUS_FLOWOPS_TEST_MACHINE_A": machines[0], "NEXUS_FLOWOPS_TEST_MACHINE_B": machines[1],
               "NEXUS_FLOWOPS_TEST_STAFF_A1": staff1, "NEXUS_FLOWOPS_TEST_STAFF_A2": staff2,
               "NEXUS_FLOWOPS_TEST_STAFF_A_OTHER_DEPARTMENT": other, "NEXUS_FLOWOPS_TEST_STAFF_B": staff_b}
    with os.fdopen(os.open(local_env, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), "w") as output:
        for name, value in exports.items():
            output.write(f"export {name}={shlex.quote(value)}\n")
    print("Saved temporary credential exports locally (0600).")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, OSError, subprocess.CalledProcessError) as error:
        print(f"FAIL {error}", file=sys.stderr)
        sys.exit(1)
