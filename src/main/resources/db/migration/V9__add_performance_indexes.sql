-- ════════════════════════════════════════════════════════════════
-- INDEX USERS
-- ════════════════════════════════════════════════════════════════




-- Pour les requêtes filtrées par organization
CREATE INDEX IF NOT EXISTS idx_users_org_id
  ON users(organization_id);




-- Pour les requêtes filtrées par organization + role
CREATE INDEX IF NOT EXISTS idx_users_org_role
  ON users(organization_id, role);




-- Pour les requêtes avec WHERE active
CREATE INDEX IF NOT EXISTS idx_users_org_active
  ON users(organization_id, active);




-- Pour l'ORDER BY created_at DESC (utilisé dans findUsersWithStats)
CREATE INDEX IF NOT EXISTS idx_users_org_created
  ON users(organization_id, created_at DESC);




-- Pour les recherches par username
CREATE INDEX IF NOT EXISTS idx_users_username
  ON users(username);




-- Pour les recherches par email
CREATE INDEX IF NOT EXISTS idx_users_email
  ON users(email);




-- ════════════════════════════════════════════════════════════════
-- INDEX COURSE_ENROLLMENTS (CRITIQUE pour éliminer N+1)
-- ════════════════════════════════════════════════════════════════




-- Pour COUNT(*) WHERE student_id = ? (N+1 problem)
CREATE INDEX IF NOT EXISTS idx_enrollments_student
  ON course_enrollments(student_id);




-- Pour COUNT(*) WHERE student_id = ? AND completed = true
CREATE INDEX IF NOT EXISTS idx_enrollments_student_completed
  ON course_enrollments(student_id, completed);




-- Pour les requêtes organization + student
CREATE INDEX IF NOT EXISTS idx_enrollments_org_student
  ON course_enrollments(organization_id, student_id);




-- Pour les requêtes organization + course
CREATE INDEX IF NOT EXISTS idx_enrollments_org_course
  ON course_enrollments(organization_id, course_id);




-- Pour filtrer les cours complétés
CREATE INDEX IF NOT EXISTS idx_enrollments_completed
  ON course_enrollments(completed);




-- ════════════════════════════════════════════════════════════════
-- INDEX COURSES
-- ════════════════════════════════════════════════════════════════




-- Pour les requêtes organization
CREATE INDEX IF NOT EXISTS idx_courses_org_id
  ON courses(organization_id);




-- Pour les requêtes organization + author
CREATE INDEX IF NOT EXISTS idx_courses_org_author
  ON courses(organization_id, author_id);




-- Pour filtrer les cours publiés
CREATE INDEX IF NOT EXISTS idx_courses_org_published
  ON courses(organization_id, published);




-- Pour les recherches par catégorie
CREATE INDEX IF NOT EXISTS idx_courses_category
  ON courses(category);




-- Pour les recherches par slug
CREATE INDEX IF NOT EXISTS idx_courses_slug
  ON courses(slug);




-- ════════════════════════════════════════════════════════════════
-- INDEX COURSE_SECTIONS
-- ════════════════════════════════════════════════════════════════




-- Pour récupérer les sections d'un cours
CREATE INDEX IF NOT EXISTS idx_sections_org_course
  ON course_sections(organization_id, course_id);




-- Pour l'ordre des sections
CREATE INDEX IF NOT EXISTS idx_sections_course_order
  ON course_sections(course_id, order_index);




-- ════════════════════════════════════════════════════════════════
-- INDEX LESSONS
-- ════════════════════════════════════════════════════════════════




-- Pour récupérer les leçons d'une section
CREATE INDEX IF NOT EXISTS idx_lessons_org_section
  ON lessons(organization_id, section_id);




-- Pour l'ordre des leçons
CREATE INDEX IF NOT EXISTS idx_lessons_section_order
  ON lessons(section_id, order_index);




-- Pour filtrer par type
CREATE INDEX IF NOT EXISTS idx_lessons_type
  ON lessons(type);




-- ════════════════════════════════════════════════════════════════
-- INDEX LESSON_PROGRESS
-- ════════════════════════════════════════════════════════════════




-- Pour récupérer la progression d'un étudiant
CREATE INDEX IF NOT EXISTS idx_progress_org_student
  ON lesson_progress(organization_id, student_id);




-- Pour vérifier la progression d'une leçon spécifique
CREATE INDEX IF NOT EXISTS idx_progress_student_lesson
  ON lesson_progress(student_id, lesson_id);




-- Pour compter les leçons complétées
CREATE INDEX IF NOT EXISTS idx_progress_student_completed
  ON lesson_progress(student_id, completed);




-- Pour filtrer par leçon
CREATE INDEX IF NOT EXISTS idx_progress_lesson
  ON lesson_progress(lesson_id);




-- ════════════════════════════════════════════════════════════════
-- INDEX REFRESH_TOKENS
-- ════════════════════════════════════════════════════════════════




-- Pour la validation du token
CREATE INDEX IF NOT EXISTS idx_refresh_token_hash
  ON refresh_tokens(token_hash);




-- Pour récupérer les tokens d'un user
CREATE INDEX IF NOT EXISTS idx_refresh_user_id
  ON refresh_tokens(user_id);




-- Pour nettoyer les tokens expirés
CREATE INDEX IF NOT EXISTS idx_refresh_expires
  ON refresh_tokens(expires_at);




-- Pour filtrer les tokens révoqués
CREATE INDEX IF NOT EXISTS idx_refresh_revoked
  ON refresh_tokens(revoked);
