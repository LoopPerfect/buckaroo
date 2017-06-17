package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ProjectNotFoundException extends DependencyResolverException {

    public final ImmutableList<RecipeIdentifier> closeIds;

    private static String createMessage(final RecipeIdentifier id, final ImmutableList<RecipeIdentifier> closeIds){
        StringBuilder builder = new StringBuilder("There is no package called")
            .append(" \"")
            .append(id.encode())
            .append("\".");

        if( closeIds != null && !closeIds.isEmpty())
        {
            builder.append("\n")
                .append("Do you mean ")
                .append(closeIds.stream()
                        .flatMap( x -> Stream.of(",", x.toString()))
                        .skip(1)
                        .collect(Collectors.joining()))
                .append(" ?");
        }

        return builder.toString();
    }

    public ProjectNotFoundException(final RecipeIdentifier id){
        this(id, ImmutableList.of());
    }

    public ProjectNotFoundException(final RecipeIdentifier id, final ImmutableList<RecipeIdentifier> closeIds){
        super(id, createMessage(id, closeIds));
        this.closeIds = closeIds;
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

        return Objects.equals(id, other.id) && Objects.equals(closeIds, other.closeIds);
    }
}
