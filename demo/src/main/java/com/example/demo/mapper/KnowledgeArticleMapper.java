package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {
    @Select("""
            <script>
            SELECT ka.* FROM knowledge_article ka
            WHERE ka.tenant_id = #{tenantId}
              AND (
                #{manage} = TRUE
                OR (ka.status = 'DRAFT' AND ka.author_id = #{userId})
                OR (ka.status = 'PUBLISHED' AND ka.visibility = 'TENANT')
                OR (ka.status = 'PUBLISHED' AND ka.visibility = 'DEPARTMENT' AND ka.department_id = #{departmentId})
              )
              <if test="categoryId != null">AND ka.category_id = #{categoryId}</if>
              <if test="keyword != null and keyword != ''">AND MATCH(ka.title, ka.summary, ka.content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)</if>
            ORDER BY ka.updated_at DESC
            </script>
            """)
    Page<KnowledgeArticle> selectVisiblePage(Page<KnowledgeArticle> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("userId") Long userId,
                                              @Param("departmentId") Long departmentId,
                                              @Param("manage") boolean manage,
                                              @Param("categoryId") Long categoryId,
                                              @Param("keyword") String keyword);
}
