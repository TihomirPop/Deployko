package hr.tvz.popovic.deployko.application.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageCommitShaTest {

    @Test
    void known_commit_sha_trims_value() {
        ImageCommitSha.Known commitSha = new ImageCommitSha.Known("  f5a1c2d  ");

        assertThat(commitSha.value()).isEqualTo("f5a1c2d");
    }

    @Test
    void known_commit_sha_rejects_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ImageCommitSha.Known(null))
                .withMessage("value must not be null");
    }

    @Test
    void known_commit_sha_rejects_blank() {
        assertThatThrownBy(() -> new ImageCommitSha.Known(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must not be blank");
    }

    @Test
    void unknown_commit_sha_represents_missing_revision_label() {
        ImageCommitSha commitSha = new ImageCommitSha.Unknown();

        assertThat(commitSha).isInstanceOf(ImageCommitSha.Unknown.class);
    }
}
