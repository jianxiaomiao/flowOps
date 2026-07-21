package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.converter.KnowledgeConverter;
import com.example.demo.dto.knowledge.*;
import com.example.demo.service.KnowledgeService;
import com.example.demo.vo.KnowledgeArticleResponse;
import com.example.demo.vo.KnowledgeCategoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 知识库协议层：所有可信租户、权限范围和文章状态规则均由 KnowledgeService 执行。 */
@Validated
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('knowledge:read','knowledge:write','knowledge:manage')")
    public Result<List<KnowledgeCategoryResponse>> categories() { return Result.success(knowledgeService.listCategories().stream().map(KnowledgeConverter::toResponse).toList()); }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<KnowledgeCategoryResponse> createCategory(@Valid @RequestBody KnowledgeCategoryCreateRequest request) { return Result.success(KnowledgeConverter.toResponse(knowledgeService.createCategory(request))); }

    @PutMapping("/categories")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public Result<KnowledgeCategoryResponse> updateCategory(@Valid @RequestBody KnowledgeCategoryUpdateRequest request) { return Result.success(KnowledgeConverter.toResponse(knowledgeService.updateCategory(request))); }

    @GetMapping("/articles/page")
    @PreAuthorize("hasAnyAuthority('knowledge:read','knowledge:write','knowledge:manage')")
    public Result<IPage<KnowledgeArticleResponse>> page(@Valid @ModelAttribute KnowledgeArticlePageQuery query) { return Result.success(knowledgeService.pageVisibleArticles(query).convert(KnowledgeConverter::toResponse)); }

    @GetMapping("/articles/{id}")
    @PreAuthorize("hasAnyAuthority('knowledge:read','knowledge:write','knowledge:manage')")
    public Result<KnowledgeArticleResponse> get(@PathVariable @Positive Long id) { return Result.success(KnowledgeConverter.toResponse(knowledgeService.getVisibleArticle(id))); }

    @PostMapping("/articles")
    @PreAuthorize("hasAnyAuthority('knowledge:write','knowledge:manage')")
    public Result<KnowledgeArticleResponse> createArticle(@Valid @RequestBody KnowledgeArticleCreateRequest request) { return Result.success(KnowledgeConverter.toResponse(knowledgeService.createArticle(request))); }

    @PutMapping("/articles")
    @PreAuthorize("hasAnyAuthority('knowledge:write','knowledge:manage')")
    public Result<KnowledgeArticleResponse> updateArticle(@Valid @RequestBody KnowledgeArticleUpdateRequest request) { return Result.success(KnowledgeConverter.toResponse(knowledgeService.updateArticle(request))); }

    @PostMapping("/articles/{id}/publish")
    @PreAuthorize("hasAnyAuthority('knowledge:write','knowledge:manage')")
    public Result<KnowledgeArticleResponse> publish(@PathVariable @Positive Long id, @Valid @RequestBody KnowledgeArticleStateRequest request) { return Result.success(KnowledgeConverter.toResponse(knowledgeService.publishArticle(id, request))); }

    @PostMapping("/articles/{id}/archive")
    @PreAuthorize("hasAnyAuthority('knowledge:write','knowledge:manage')")
    public Result<KnowledgeArticleResponse> archive(@PathVariable @Positive Long id, @Valid @RequestBody KnowledgeArticleStateRequest request) { return Result.success(KnowledgeConverter.toResponse(knowledgeService.archiveArticle(id, request))); }

    @PostMapping("/articles/{articleId}/tickets/{ticketId}")
    @PreAuthorize("hasAnyAuthority('knowledge:write','knowledge:manage') and @ticketReadScope.canRead(#ticketId)")
    public Result<Void> linkTicket(@PathVariable @Positive Long articleId, @PathVariable @Positive Long ticketId, @Valid @RequestBody KnowledgeArticleTicketCreateRequest request) { knowledgeService.linkTicket(articleId, ticketId, request); return Result.success(); }
}
