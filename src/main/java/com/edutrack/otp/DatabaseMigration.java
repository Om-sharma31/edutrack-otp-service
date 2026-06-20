package com.edutrack.otp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Applies one-time DDL fixes that Hibernate ddl-auto=update cannot handle
 * (e.g. dropping NOT NULL constraints on existing columns).
 * Safe to run on every startup — each statement is idempotent.
 */
@Component
public class DatabaseMigration {

    @Autowired
    private DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void applyMigrations() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Allow students to be created without an email address.
            // Hibernate ddl-auto=update won't drop an existing NOT NULL constraint,
            // so we do it explicitly here.
            stmt.execute("ALTER TABLE users ALTER COLUMN email DROP NOT NULL");
            System.out.println("[EduTrack Migration] email column is now nullable — OK");

        } catch (Exception e) {
            // Silently ignore: constraint was already dropped on a previous startup,
            // or the table doesn't exist yet (first run, Hibernate creates it correctly).
            System.out.println("[EduTrack Migration] email nullable — already applied or table not yet created: " + e.getMessage());
        }
    }
}
