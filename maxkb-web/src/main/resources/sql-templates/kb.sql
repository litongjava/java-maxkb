--# kb.hit_test_by_dataset_id
SELECT
  sub.document_name,
  sub.dataset_name,
  sub.create_time,
  sub.update_time,
  sub.id,
  sub.content,
  sub.title,
  sub.status,
  sub.hit_num,
  sub.is_active,
  sub.dataset_id,
  sub.document_id,
  sub.similarity,
  sub.similarity AS comprehensive_score
FROM (
  SELECT
    d.name AS document_name,
    ds.name AS dataset_name,
    p.create_time,
    p.update_time,
    p.id,
    p.content,
    p.title,
    p.status,
    p.hit_num,
    p.is_active,
    p.dataset_id,
    p.document_id,
    (1 - (p.embedding <=> ?::vector)) AS similarity
  FROM
    max_kb_paragraph p
  JOIN
    max_kb_document d ON p.document_id = d.id
  JOIN
    max_kb_dataset ds ON p.dataset_id = ds.id
  WHERE
    p.is_active = TRUE
    AND p.deleted = 0
    AND ds.deleted = 0
    AND p.dataset_id = ?
) sub
WHERE
  sub.similarity > ?
ORDER BY
  sub.similarity DESC
LIMIT ?;

--# kb.hit_test_by_dataset_ids_with_max_kb_embedding_cache
SELECT
  sub.document_name,
  sub.dataset_name,
  sub.create_time,
  sub.update_time,
  sub.id,
  sub.content,
  sub.title,
  sub.status,
  sub.hit_num,
  sub.is_active,
  sub.dataset_id,
  sub.document_id,
  sub.similarity,
  sub.similarity AS comprehensive_score
FROM (
  SELECT
    d.name AS document_name,
    ds.name AS dataset_name,
    p.create_time,
    p.update_time,
    p.id,
    p.content,
    p.title,
    p.status,
    p.hit_num,
    p.is_active,
    p.dataset_id,
    p.document_id,
    (1 - (p.embedding <=> c.v)) AS similarity
  FROM
    max_kb_paragraph p
  JOIN
    max_kb_document d ON p.document_id = d.id
  JOIN
    max_kb_dataset ds ON p.dataset_id = ds.id
  JOIN
    max_kb_embedding_cache c ON c.id = ?
  WHERE
    p.is_active = TRUE
    AND p.deleted = 0
    AND ds.deleted = 0
    AND p.dataset_id IN (#(in_list))
) sub
WHERE
  sub.similarity > ?
ORDER BY
  sub.similarity DESC
LIMIT ?;

--# kb.hit_test_by_dataset_id_with_max_kb_embedding_cache
SELECT
  sub.document_name,
  sub.dataset_name,
  sub.create_time,
  sub.update_time,
  sub.id,
  sub.content,
  sub.title,
  sub.status,
  sub.hit_num,
  sub.is_active,
  sub.dataset_id,
  sub.document_id,
  sub.similarity,
  sub.similarity AS comprehensive_score
FROM (
  SELECT
    d.name AS document_name,
    ds.name AS dataset_name,
    p.create_time,
    p.update_time,
    p.id,
    p.content,
    p.title,
    p.status,
    p.hit_num,
    p.is_active,
    p.dataset_id,
    p.document_id,
    (1 - (p.embedding <=> c.v)) AS similarity
  FROM
    max_kb_paragraph p
  JOIN
    max_kb_document d ON p.document_id = d.id
  JOIN
    max_kb_dataset ds ON p.dataset_id = ds.id
  JOIN
    max_kb_embedding_cache c ON c.id = ?
  WHERE
    p.is_active = TRUE
    AND p.deleted = 0
    AND ds.deleted = 0
    AND p.dataset_id = ?
) sub
WHERE
  sub.similarity > ?
ORDER BY
  sub.similarity DESC
LIMIT ?;
--# kb.search_sentense_related_paragraph__with_dataset_ids
SELECT DISTINCT
  sub.id,
  sub.content,
  sub.title,
  sub.status,
  sub.hit_num,
  sub.is_active,
  sub.dataset_id,
  sub.document_id,
  sub.dataset_name,
  sub.document_name,
  sub.similarity,
  sub.similarity AS comprehensive_score
FROM (
  SELECT
    d.name AS document_name,
    ds.name AS dataset_name,
    s.id,
    s.content,
    CASE 
      WHEN s.type = 1 THEN p.title
      ELSE ''
    END AS title,
    CASE 
      WHEN s.type = 1 THEN p.status
      ELSE '1'
    END AS status,
    s.hit_num,
    CASE 
      WHEN s.type = 1 THEN p.is_active
      ELSE true
    END AS is_active,
    s.dataset_id,
    s.document_id,
    (1 - (s.embedding <=> c.v)) AS similarity
  FROM
    max_kb_sentence s
  JOIN
    max_kb_embedding_cache c ON c.id = ?
  JOIN
    max_kb_dataset ds ON s.dataset_id = ds.id
  JOIN
    max_kb_document d ON s.document_id = d.id
  LEFT JOIN
    max_kb_paragraph p ON s.type = 1 AND s.paragraph_id = p.id
  WHERE
    (
      (s.type = 1 AND p.is_active = TRUE) 
      OR 
      (s.type = 2 AND d.is_active = TRUE)
    )
    AND s.deleted = 0
    AND ds.deleted = 0
    AND s.dataset_id = ANY (?)
) sub
WHERE
  sub.similarity > ?
ORDER BY
  sub.similarity DESC
LIMIT ?;

--# kb.search_paragraph_with_dataset_ids
SELECT
  sub.id,
  sub.content,
  sub.title,
  sub.status,
  sub.hit_num,
  sub.is_active,
  sub.dataset_id,
  sub.document_id,
  sub.dataset_name,
  sub.document_name,
  sub.similarity,
  sub.similarity AS comprehensive_score
FROM (
  SELECT
    d.name AS document_name,
    ds.name AS dataset_name,
    p.id,
    p.content,
    p.title,
    p.status,
    p.hit_num,
    p.is_active,
    p.dataset_id,
    p.document_id,
    (1 - (p.embedding <=> c.v)) AS similarity
  FROM
    max_kb_paragraph p
  JOIN
    max_kb_document d ON p.document_id = d.id
  JOIN
    max_kb_dataset ds ON p.dataset_id = ds.id
  JOIN
    max_kb_embedding_cache c ON c.id = ?
  WHERE
    p.is_active = TRUE
    AND p.deleted = 0
    AND ds.deleted = 0
    AND p.dataset_id = ANY (?)
) sub
WHERE
  sub.similarity > ?
ORDER BY
  sub.similarity DESC
LIMIT ?;
--# kb.list_database_id_by_application_id
SELECT 
    mapping.dataset_id 
FROM 
    max_kb_application_dataset_mapping AS mapping
INNER JOIN 
    max_kb_dataset AS dataset 
    ON mapping.dataset_id = dataset.id
WHERE 
    mapping.application_id = ?
    AND dataset.deleted = 0;
