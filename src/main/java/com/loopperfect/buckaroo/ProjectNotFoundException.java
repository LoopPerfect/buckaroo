package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * Created by gaetano on 16/02/17.
 */
public final class ProjectNotFoundException extends DependencyResolverException {

    public ProjectNotFoundException(final Identifier id){
        super(id, "Project not found " + id.name);
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
