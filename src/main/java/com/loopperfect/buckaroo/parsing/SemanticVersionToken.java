package com.loopperfect.buckaroo.parsing;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.SemanticVersion;

import java.util.Objects;

public final class SemanticVersionToken implements Token {

    public final SemanticVersion semanticVersion;

    private SemanticVersionToken(final SemanticVersion semanticVersion) {
        this.semanticVersion = Preconditions.checkNotNull(semanticVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semanticVersion);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof SemanticVersionToken)) {
            return false;
        }

        final SemanticVersionToken other = (SemanticVersionToken) obj;

        return Objects.equals(semanticVersion, other.semanticVersion);
    }

    public static SemanticVersionToken of(final SemanticVersion semanticVersion) {
        return new SemanticVersionToken(semanticVersion);
    }
}
