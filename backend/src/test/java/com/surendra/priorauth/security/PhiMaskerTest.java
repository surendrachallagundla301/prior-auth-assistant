package com.surendra.priorauth.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhiMaskerTest {

    @Test
    void masksMemberIdKeepingLastFour() {
        assertThat(PhiMasker.maskMemberId("ABC123456789")).isEqualTo("********6789");
    }

    @Test
    void masksNameKeepingInitials() {
        assertThat(PhiMasker.maskName("Jane Doe")).isEqualTo("J*** D**");
    }

    @Test
    void masksDateCompletely() {
        assertThat(PhiMasker.maskDate("1985-04-12")).isEqualTo("****-**-**");
    }

    @Test
    void scrubsIdentifiersFromFreeText() {
        String scrubbed = PhiMasker.scrub("member ABC123456789 born 1985-04-12 requested MRI");

        assertThat(scrubbed).doesNotContain("123456789").doesNotContain("1985-04-12");
        assertThat(scrubbed).isEqualTo("member ABC*****6789 born ****-**-** requested MRI");
    }
}
