package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.audit.AuditOperation;
import com.example.demo.common.BusinessException;
import com.example.demo.converter.KnowledgeConverter;
import com.example.demo.dto.knowledge.*;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.security.TicketReadScope;
import com.example.demo.security.TenantContext;
import com.example.demo.service.KnowledgeService;
import com.example.demo.messaging.OutboxEventService;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeArticleMapper, KnowledgeArticle> implements KnowledgeService {
    private static final String ACTIVE = "ACTIVE";
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeArticleTicketMapper ticketLinkMapper;
    private final UserAccountMapper userAccountMapper;
    private final TenantScopeValidator tenantScopeValidator;
    private final TenantContext tenantContext;
    private final CurrentUserProvider currentUserProvider;
    private final TicketReadScope ticketReadScope;
    private final OutboxEventService outboxEventService;

    public KnowledgeServiceImpl(KnowledgeCategoryMapper categoryMapper, KnowledgeArticleTicketMapper ticketLinkMapper,
                                UserAccountMapper userAccountMapper, TenantScopeValidator tenantScopeValidator,
                                TenantContext tenantContext, CurrentUserProvider currentUserProvider,
                                TicketReadScope ticketReadScope, OutboxEventService outboxEventService) {
        this.categoryMapper = categoryMapper; this.ticketLinkMapper = ticketLinkMapper; this.userAccountMapper = userAccountMapper;
        this.tenantScopeValidator = tenantScopeValidator; this.tenantContext = tenantContext;
        this.currentUserProvider = currentUserProvider; this.ticketReadScope = ticketReadScope;
        this.outboxEventService = outboxEventService;
    }

    @Override @Transactional @AuditOperation(module = "KNOWLEDGE", operation = "CREATE_CATEGORY", resourceType = "KNOWLEDGE_CATEGORY")
    public KnowledgeCategory createCategory(KnowledgeCategoryCreateRequest request) {
        Long tenantId = tenantContext.tenantId(); tenantScopeValidator.requireActiveTenant(tenantId);
        String code = normalizeCode(request.getCategoryCode());
        if (categoryMapper.selectCount(Wrappers.lambdaQuery(KnowledgeCategory.class).eq(KnowledgeCategory::getTenantId, tenantId).eq(KnowledgeCategory::getCategoryCode, code)) > 0) throw new BusinessException(30001, "分类编码已存在");
        if (request.getParentId() != null) requireCategory(tenantId, request.getParentId());
        KnowledgeCategory category = KnowledgeConverter.toEntity(request);
        category.setTenantId(tenantId); category.setCategoryCode(code); category.setStatus(ACTIVE); category.setCreatedBy(tenantContext.userId()); category.setUpdatedBy(tenantContext.userId());
        if (categoryMapper.insert(category) != 1) throw new IllegalStateException("创建知识分类失败");
        return category;
    }

    @Override @Transactional @AuditOperation(module = "KNOWLEDGE", operation = "UPDATE_CATEGORY", resourceType = "KNOWLEDGE_CATEGORY")
    public KnowledgeCategory updateCategory(KnowledgeCategoryUpdateRequest request) {
        Long tenantId = tenantContext.tenantId(); KnowledgeCategory category = requireCategory(tenantId, request.getId());
        if (request.getParentId() != null) { if (request.getParentId().equals(category.getId())) throw new BusinessException(10001, "分类不能以自身为父分类"); requireCategory(tenantId, request.getParentId()); }
        category.setParentId(request.getParentId()); category.setCategoryName(request.getCategoryName().trim()); category.setDescription(blankToNull(request.getDescription())); category.setSortOrder(request.getSortOrder()); category.setVersion(request.getVersion()); category.setUpdatedBy(tenantContext.userId());
        if (categoryMapper.updateById(category) != 1) throw new BusinessException(30002, "知识分类已被其他操作修改，请刷新后重试");
        return category;
    }

    @Override
    public List<KnowledgeCategory> listCategories() {
        return categoryMapper.selectList(Wrappers.lambdaQuery(KnowledgeCategory.class).eq(KnowledgeCategory::getTenantId, tenantContext.tenantId()).eq(KnowledgeCategory::getStatus, ACTIVE).orderByAsc(KnowledgeCategory::getSortOrder).orderByAsc(KnowledgeCategory::getId));
    }

    @Override @Transactional @AuditOperation(module = "KNOWLEDGE", operation = "CREATE_ARTICLE", resourceType = "KNOWLEDGE_ARTICLE")
    public KnowledgeArticle createArticle(KnowledgeArticleCreateRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser(); tenantScopeValidator.requireActiveTenant(user.tenantId());
        requireCategory(user.tenantId(), request.getCategoryId()); KnowledgeArticle article = KnowledgeConverter.toEntity(request);
        applyArticleInput(article, request, user.tenantId()); article.setTenantId(user.tenantId()); article.setAuthorId(user.userId()); article.setStatus("DRAFT"); article.setUpdatedBy(user.userId());
        if (!save(article)) throw new IllegalStateException("创建知识文章失败");
        return article;
    }

    @Override @Transactional @AuditOperation(module = "KNOWLEDGE", operation = "UPDATE_ARTICLE", resourceType = "KNOWLEDGE_ARTICLE")
    public KnowledgeArticle updateArticle(KnowledgeArticleUpdateRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser(); KnowledgeArticle article = requireArticle(user.tenantId(), request.getId()); requireCanEdit(user, article);
        if ("ARCHIVED".equals(article.getStatus())) throw new BusinessException(30001, "已归档文章不能编辑");
        requireCategory(user.tenantId(), request.getCategoryId()); applyArticleInput(article, request, user.tenantId()); article.setVersion(request.getVersion()); article.setUpdatedBy(user.userId());
        if (!updateById(article)) throw new BusinessException(30002, "知识文章已被其他操作修改，请刷新后重试");
        
        if ("PUBLISHED".equals(article.getStatus())) {
            outboxEventService.enqueue(user.tenantId(), "KNOWLEDGE_ARTICLE", article.getId(), "KNOWLEDGE_ARTICLE_UPDATED", 
                java.util.Map.of("articleId", article.getId(), "title", article.getTitle(), "content", article.getContent(), "version", article.getVersion()));
        }
        return article;
    }

    @Override
    public KnowledgeArticle getVisibleArticle(Long articleId) { KnowledgeArticle article = requireArticle(tenantContext.tenantId(), articleId); requireCanRead(currentUserProvider.requireCurrentUser(), article); return article; }

    @Override
    public Page<KnowledgeArticle> pageVisibleArticles(KnowledgeArticlePageQuery query) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        return baseMapper.selectVisiblePage(new Page<>(query.getPageNum(), query.getPageSize()), user.tenantId(), user.userId(), currentDepartmentId(user), canManage(user), query.getCategoryId(), blankToNull(query.getKeyword()));
    }

    @Override @Transactional @AuditOperation(module = "KNOWLEDGE", operation = "PUBLISH_ARTICLE", resourceType = "KNOWLEDGE_ARTICLE")
    public KnowledgeArticle publishArticle(Long articleId, KnowledgeArticleStateRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser(); KnowledgeArticle article = requireArticle(user.tenantId(), articleId); requireCanEdit(user, article);
        if (!"DRAFT".equals(article.getStatus())) throw new BusinessException(30001, "只有草稿可以发布");
        article.setStatus("PUBLISHED"); article.setPublishedAt(LocalDateTime.now()); article.setVersion(request.getVersion()); article.setUpdatedBy(user.userId());
        if (!updateById(article)) throw new BusinessException(30002, "知识文章已被其他操作修改，请刷新后重试");
        
        outboxEventService.enqueue(user.tenantId(), "KNOWLEDGE_ARTICLE", article.getId(), "KNOWLEDGE_ARTICLE_PUBLISHED", 
            java.util.Map.of("articleId", article.getId(), "title", article.getTitle(), "content", article.getContent(), "version", article.getVersion()));
            
        return article;
    }

    @Override @Transactional @AuditOperation(module = "KNOWLEDGE", operation = "ARCHIVE_ARTICLE", resourceType = "KNOWLEDGE_ARTICLE")
    public KnowledgeArticle archiveArticle(Long articleId, KnowledgeArticleStateRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser(); KnowledgeArticle article = requireArticle(user.tenantId(), articleId); requireCanEdit(user, article);
        if ("ARCHIVED".equals(article.getStatus())) return article;
        article.setStatus("ARCHIVED"); article.setVersion(request.getVersion()); article.setUpdatedBy(user.userId());
        if (!updateById(article)) throw new BusinessException(30002, "知识文章已被其他操作修改，请刷新后重试");
        
        outboxEventService.enqueue(user.tenantId(), "KNOWLEDGE_ARTICLE", article.getId(), "KNOWLEDGE_ARTICLE_ARCHIVED", 
            java.util.Map.of("articleId", article.getId()));
            
        return article;
    }

    @Override @Transactional @AuditOperation(module = "KNOWLEDGE", operation = "LINK_TICKET", resourceType = "KNOWLEDGE_ARTICLE_TICKET")
    public void linkTicket(Long articleId, Long ticketId, KnowledgeArticleTicketCreateRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser(); if (!ticketReadScope.canRead(ticketId)) throw new BusinessException(20001, "工单不存在或无权访问");
        getVisibleArticle(articleId); String relationType = request.getRelationType().trim().toUpperCase(Locale.ROOT);
        if (!("REFERENCE".equals(relationType) || "SOLUTION".equals(relationType))) throw new BusinessException(10001, "relationType 仅支持 REFERENCE 或 SOLUTION");
        boolean exists = ticketLinkMapper.selectCount(Wrappers.lambdaQuery(KnowledgeArticleTicket.class).eq(KnowledgeArticleTicket::getTenantId, user.tenantId()).eq(KnowledgeArticleTicket::getArticleId, articleId).eq(KnowledgeArticleTicket::getTicketId, ticketId)) > 0;
        if (!exists) { KnowledgeArticleTicket link = new KnowledgeArticleTicket(); link.setTenantId(user.tenantId()); link.setArticleId(articleId); link.setTicketId(ticketId); link.setRelationType(relationType); link.setCreatedBy(user.userId()); if (ticketLinkMapper.insert(link) != 1) throw new IllegalStateException("关联知识文章失败"); }
    }

    @Override
    public List<KnowledgeArticle> listVisibleArticlesForTicket(Long ticketId) {
        if (!ticketReadScope.canRead(ticketId)) throw new BusinessException(20001, "工单不存在或无权访问");
        return ticketLinkMapper.selectList(Wrappers.lambdaQuery(KnowledgeArticleTicket.class).eq(KnowledgeArticleTicket::getTenantId, tenantContext.tenantId()).eq(KnowledgeArticleTicket::getTicketId, ticketId).orderByDesc(KnowledgeArticleTicket::getCreatedAt)).stream().map(KnowledgeArticleTicket::getArticleId).map(id -> { try { return getVisibleArticle(id); } catch (BusinessException ignored) { return null; } }).filter(java.util.Objects::nonNull).toList();
    }

    private KnowledgeCategory requireCategory(Long tenantId, Long categoryId) { KnowledgeCategory category = categoryMapper.selectOne(Wrappers.lambdaQuery(KnowledgeCategory.class).eq(KnowledgeCategory::getTenantId, tenantId).eq(KnowledgeCategory::getId, categoryId).eq(KnowledgeCategory::getStatus, ACTIVE)); if (category == null) throw new BusinessException(20001, "知识分类不存在、已停用或不属于当前租户"); return category; }
    private KnowledgeArticle requireArticle(Long tenantId, Long articleId) { if (articleId == null || articleId <= 0) throw new BusinessException(10001, "articleId 必须是正整数"); KnowledgeArticle article = getOne(Wrappers.lambdaQuery(KnowledgeArticle.class).eq(KnowledgeArticle::getTenantId, tenantId).eq(KnowledgeArticle::getId, articleId)); if (article == null) throw new BusinessException(20001, "知识文章不存在"); return article; }
    private void applyArticleInput(KnowledgeArticle article, KnowledgeArticleCreateRequest request, Long tenantId) { String visibility = request.getVisibility().trim().toUpperCase(Locale.ROOT); if (!("TENANT".equals(visibility) || "DEPARTMENT".equals(visibility))) throw new BusinessException(10001, "visibility 仅支持 TENANT 或 DEPARTMENT"); if ("DEPARTMENT".equals(visibility)) tenantScopeValidator.requireActiveDepartment(tenantId, request.getDepartmentId()); article.setCategoryId(request.getCategoryId()); article.setTitle(request.getTitle().trim()); article.setSummary(blankToNull(request.getSummary())); article.setContent(request.getContent().trim()); article.setVisibility(visibility); article.setDepartmentId("DEPARTMENT".equals(visibility) ? request.getDepartmentId() : null); }
    private void requireCanEdit(CurrentUser user, KnowledgeArticle article) { if (!canManage(user) && !user.userId().equals(article.getAuthorId())) throw new BusinessException(10003, "无权修改其他用户的知识文章"); }
    private void requireCanRead(CurrentUser user, KnowledgeArticle article) { if (canManage(user) || ("DRAFT".equals(article.getStatus()) && user.userId().equals(article.getAuthorId())) || ("PUBLISHED".equals(article.getStatus()) && "TENANT".equals(article.getVisibility())) || ("PUBLISHED".equals(article.getStatus()) && "DEPARTMENT".equals(article.getVisibility()) && currentDepartmentId(user).equals(article.getDepartmentId()))) return; throw new BusinessException(20001, "知识文章不存在"); }
    private boolean canManage(CurrentUser user) { return user.authorities().contains("knowledge:manage"); }
    private Long currentDepartmentId(CurrentUser user) { UserAccount account = userAccountMapper.selectOne(Wrappers.lambdaQuery(UserAccount.class).select(UserAccount::getDepartmentId).eq(UserAccount::getTenantId, user.tenantId()).eq(UserAccount::getId, user.userId())); return account == null || account.getDepartmentId() == null ? -1L : account.getDepartmentId(); }
    private String normalizeCode(String value) { if (!StringUtils.hasText(value)) throw new BusinessException(10001, "categoryCode 不能为空"); String code = value.trim().toUpperCase(Locale.ROOT); if (!code.matches("^[A-Z][A-Z0-9_]{0,63}$")) throw new BusinessException(10001, "categoryCode 只能包含大写字母、数字和下划线，且必须以字母开头"); return code; }
    private String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
