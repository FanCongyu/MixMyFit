package com.fan.mixmyfit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MigrationScriptTest {

    @Test
    void initialMigrationScriptDefinesRequiredTables() throws IOException {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__initial_schema.sql");

        assertThat(migration.exists()).isTrue();

        String sql = migration.getContentAsString(StandardCharsets.UTF_8).toLowerCase();
        assertThat(sql)
                .contains("create table users")
                .contains("create table categories")
                .contains("create table clothes")
                .contains("create table clothing_seasons")
                .contains("create table clothing_tags")
                .contains("create table clothing_tag_links")
                .contains("create table outfit_tags")
                .contains("create table outfits")
                .contains("create table outfit_seasons")
                .contains("create table outfit_tag_links")
                .contains("create table outfit_items");
    }
}
