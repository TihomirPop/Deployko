package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VolumeMountsTest {

    @Test
    void creates_volume_mounts_when_entries_are_empty() {
        assertThat(VolumeMounts.empty().entries()).isEmpty();
    }

    @Test
    void copies_entries_defensively() {
        VolumeMount.Target firstTarget = new VolumeMount.Target("/app/config");
        VolumeMount.Target secondTarget = new VolumeMount.Target("/var/lib/app");
        Map<VolumeMount.Target, VolumeMount> entries = new LinkedHashMap<>();
        entries.put(
                firstTarget,
                new VolumeMount.BindMount(new VolumeMount.HostPath("/opt/deployko/config"), firstTarget, true)
        );

        VolumeMounts volumeMounts = new VolumeMounts(entries);
        entries.put(
                secondTarget,
                new VolumeMount.NamedVolumeMount(new VolumeMount.VolumeName("deployko_data"), secondTarget, false)
        );

        assertThat(volumeMounts.entries()).hasSize(1);
    }

    @Test
    void throws_when_entries_are_null() {
        assertThatThrownBy(() -> new VolumeMounts(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_target() {
        Map<VolumeMount.Target, VolumeMount> entries = new LinkedHashMap<>();
        entries.put(null, new VolumeMount.BindMount(
                new VolumeMount.HostPath("/opt/deployko/config"),
                new VolumeMount.Target("/app/config"),
                true
        ));

        assertThatThrownBy(() -> new VolumeMounts(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_volume_mount() {
        Map<VolumeMount.Target, VolumeMount> entries = new LinkedHashMap<>();
        entries.put(new VolumeMount.Target("/app/config"), null);

        assertThatThrownBy(() -> new VolumeMounts(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entry_key_does_not_match_mount_target() {
        Map<VolumeMount.Target, VolumeMount> entries = new LinkedHashMap<>();
        entries.put(
                new VolumeMount.Target("/app/config"),
                new VolumeMount.BindMount(
                        new VolumeMount.HostPath("/opt/deployko/config"),
                        new VolumeMount.Target("/other/config"),
                        true
                )
        );

        assertThatThrownBy(() -> new VolumeMounts(entries))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adds_volume_mount_when_target_is_unique() {
        VolumeMount.BindMount bindMount = new VolumeMount.BindMount(
                new VolumeMount.HostPath("/opt/deployko/config"),
                new VolumeMount.Target("/app/config"),
                true
        );

        VolumeMounts volumeMounts = VolumeMounts.empty().add(bindMount);

        assertThat(volumeMounts.entries())
                .containsExactly(Map.entry(bindMount.target(), bindMount));
    }

    @Test
    void throws_when_adding_duplicate_target() {
        VolumeMount.Target target = new VolumeMount.Target("/app/config");
        VolumeMounts volumeMounts = new VolumeMounts(Map.of(
                target, new VolumeMount.BindMount(new VolumeMount.HostPath("/opt/deployko/config"), target, true)
        ));

        assertThatThrownBy(() -> volumeMounts.add(
                new VolumeMount.NamedVolumeMount(new VolumeMount.VolumeName("deployko_config"), target, false)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaces_volume_mount_for_existing_target() {
        VolumeMount.Target target = new VolumeMount.Target("/app/config");
        VolumeMount.BindMount bindMount = new VolumeMount.BindMount(
                new VolumeMount.HostPath("/opt/deployko/config"),
                target,
                true
        );
        VolumeMount.NamedVolumeMount namedVolumeMount = new VolumeMount.NamedVolumeMount(
                new VolumeMount.VolumeName("deployko_config"),
                target,
                false
        );
        VolumeMounts volumeMounts = new VolumeMounts(Map.of(target, bindMount));

        VolumeMounts replacedVolumeMounts = volumeMounts.replace(namedVolumeMount);

        assertThat(replacedVolumeMounts.getByTarget(target)).contains(namedVolumeMount);
        assertThat(volumeMounts.getByTarget(target)).contains(bindMount);
    }

    @Test
    void throws_when_replacing_missing_target() {
        VolumeMount.NamedVolumeMount namedVolumeMount = new VolumeMount.NamedVolumeMount(
                new VolumeMount.VolumeName("deployko_config"),
                new VolumeMount.Target("/app/config"),
                false
        );

        assertThatThrownBy(() -> VolumeMounts.empty().replace(namedVolumeMount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removes_volume_mount_by_target() {
        VolumeMount.Target firstTarget = new VolumeMount.Target("/app/config");
        VolumeMount.Target secondTarget = new VolumeMount.Target("/var/lib/app");
        VolumeMount.BindMount firstMount = new VolumeMount.BindMount(
                new VolumeMount.HostPath("/opt/deployko/config"),
                firstTarget,
                true
        );
        VolumeMount.NamedVolumeMount secondMount = new VolumeMount.NamedVolumeMount(
                new VolumeMount.VolumeName("deployko_data"),
                secondTarget,
                false
        );
        VolumeMounts volumeMounts = new VolumeMounts(Map.of(
                firstTarget, firstMount,
                secondTarget, secondMount
        ));

        VolumeMounts remainingVolumeMounts = volumeMounts.removeByTarget(firstTarget);

        assertThat(remainingVolumeMounts.entries())
                .containsExactly(Map.entry(secondTarget, secondMount));
    }

    @Test
    void returns_volume_mount_by_target() {
        VolumeMount.Target target = new VolumeMount.Target("/app/config");
        VolumeMount.BindMount bindMount = new VolumeMount.BindMount(
                new VolumeMount.HostPath("/opt/deployko/config"),
                target,
                true
        );
        VolumeMounts volumeMounts = new VolumeMounts(Map.of(target, bindMount));

        assertThat(volumeMounts.getByTarget(target)).contains(bindMount);
    }

    @Test
    void returns_entries_as_map() {
        VolumeMount.Target target = new VolumeMount.Target("/app/config");
        VolumeMount.BindMount bindMount = new VolumeMount.BindMount(
                new VolumeMount.HostPath("/opt/deployko/config"),
                target,
                true
        );
        VolumeMounts volumeMounts = new VolumeMounts(Map.of(target, bindMount));

        assertThat(volumeMounts.asMap()).containsExactly(Map.entry(target, bindMount));
    }
}
