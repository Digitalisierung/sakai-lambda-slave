package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.api.dto.ArticleDeleteDTO;

import java.util.List;
import java.util.Map;

public interface BatchDeleteArticleRepository {

    List<Map<String, Object>> findByIds(List<String> articleIds);

    List<String> deleteItems(List<ArticleDeleteDTO> deleteItems);
}
