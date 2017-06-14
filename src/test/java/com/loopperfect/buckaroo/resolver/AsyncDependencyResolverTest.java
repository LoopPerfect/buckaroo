package com.loopperfect.buckaroo.resolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.sources.RecipeSources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class AsyncDependencyResolverTest {

    @Test
    public void resolveEmpty() throws Exception {

        assertEquals(
            ImmutableMap.of(),
            AsyncDependencyResolver.resolve(RecipeSources.empty(), ImmutableList.of()).result().blockingGet());
    }
}
