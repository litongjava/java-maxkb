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
