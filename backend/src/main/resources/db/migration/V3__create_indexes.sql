-- V3: Indexes for performance

CREATE INDEX idx_districts_is_active       ON districts(is_active);
CREATE INDEX idx_districts_coordinator_id  ON districts(coordinator_id);

CREATE INDEX idx_neighborhoods_district_id    ON neighborhoods(district_id);
CREATE INDEX idx_neighborhoods_coordinator_id ON neighborhoods(coordinator_id);

CREATE INDEX idx_users_email           ON users(email);
CREATE INDEX idx_users_phone           ON users(phone);
CREATE INDEX idx_users_district_id     ON users(district_id);
CREATE INDEX idx_users_neighborhood_id ON users(neighborhood_id);
CREATE INDEX idx_users_role            ON users(role);
CREATE INDEX idx_users_is_active       ON users(is_active);

CREATE INDEX idx_assembly_areas_neighborhood_id ON assembly_areas(neighborhood_id);

CREATE INDEX idx_team_memberships_user_id ON team_memberships(user_id);
CREATE INDEX idx_team_memberships_team_id ON team_memberships(team_id);

CREATE INDEX idx_events_neighborhood_id ON events(neighborhood_id);
CREATE INDEX idx_events_team_id         ON events(team_id);
CREATE INDEX idx_events_status          ON events(status);
CREATE INDEX idx_events_created_by      ON events(created_by);
CREATE INDEX idx_events_urgency         ON events(urgency);

CREATE INDEX idx_event_volunteers_event_id ON event_volunteers(event_id);
CREATE INDEX idx_event_volunteers_user_id  ON event_volunteers(user_id);

CREATE INDEX idx_documents_user_id       ON documents(user_id);
CREATE INDEX idx_documents_status        ON documents(status);
CREATE INDEX idx_documents_document_type ON documents(document_type);
CREATE INDEX idx_documents_reviewed_by   ON documents(reviewed_by);

CREATE INDEX idx_earthquake_simulations_district_id ON earthquake_simulations(district_id);
CREATE INDEX idx_earthquake_simulations_created_by  ON earthquake_simulations(created_by);

CREATE INDEX idx_sim_notif_simulation_id ON simulation_notification_logs(simulation_id);
CREATE INDEX idx_sim_notif_user_id       ON simulation_notification_logs(user_id);
CREATE INDEX idx_sim_notif_status        ON simulation_notification_logs(status);

CREATE INDEX idx_audit_logs_actor_id   ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity_id  ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_action     ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

CREATE INDEX idx_email_tokens_user_id    ON email_verification_tokens(user_id);
CREATE INDEX idx_email_tokens_token      ON email_verification_tokens(token);
CREATE INDEX idx_email_tokens_expires_at ON email_verification_tokens(expires_at);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
