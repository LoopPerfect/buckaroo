package com.loopperfect.buckaroo;

import java.util.Objects;

public final class ProjectNotFoundException extends DependencyResolverException {

    public ProjectNotFoundException(final RecipeIdentifier id){
        super(id, "Project not found " + id.encode());
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof ProjectNotFoundException)) {
            return false;
        }

        final ProjectNotFoundException other = (ProjectNotFoundException) obj;

        return Objects.equals(id, other.id);
    }
}
