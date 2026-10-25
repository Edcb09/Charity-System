UPDATE project     SET cover_img  = REPLACE(cover_img,  'http://localhost:8080', '') WHERE cover_img  LIKE '%localhost:8080%';
UPDATE project     SET proof_img  = REPLACE(proof_img,  'http://localhost:8080', '') WHERE proof_img  LIKE '%localhost:8080%';
UPDATE fund_report SET report_img = REPLACE(report_img, 'http://localhost:8080', '') WHERE report_img LIKE '%localhost:8080%';
UPDATE user        SET avatar     = REPLACE(avatar,     'http://localhost:8080', '') WHERE avatar     LIKE '%localhost:8080%';
SELECT (SELECT COUNT(*) FROM project WHERE cover_img LIKE '%localhost:8080%')
     + (SELECT COUNT(*) FROM project WHERE proof_img LIKE '%localhost:8080%')
     + (SELECT COUNT(*) FROM fund_report WHERE report_img LIKE '%localhost:8080%')
     + (SELECT COUNT(*) FROM user WHERE avatar LIKE '%localhost:8080%') AS remain_total;
