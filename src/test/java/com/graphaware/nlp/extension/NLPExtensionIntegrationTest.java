package com.graphaware.nlp.extension;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubExtension;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class NLPExtensionIntegrationTest extends NLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createPipeline(pipelineSpecification.getTextProcessor(), pipelineSpecification.getName());
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({p0})", buildSeqParameters("tokenizer"), emptyConsumer());
    }

    @Test
    public void testExtensionsAreRegistered() {
        assertNotNull(getNLPManager().getExtension(StubExtension.class));
    }

    @Test
    public void testExtensionsAreNotifiedWhenTextIsAnnotated() {
        assertNull(((StubExtension) getNLPManager().getExtension(StubExtension.class)).getSomeValue());
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().annotateTextAndPersist(
                    "Hello world",
                    "id-123",
                    pipelineSpecification
            );
            tx.success();
        }

        assertEquals("id-123", ((StubExtension) getNLPManager().getExtension(StubExtension.class)).getSomeValue());
    }

    @Test
    public void testExtensionsCanPersistData() {
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().annotateTextAndPersist(
                    "Hello world",
                    "id-123",
                    pipelineSpecification
            );
            tx.success();
        }

        executeInTransaction("MATCH (n:AnnotatedText) RETURN n", (result -> {
            assertTrue(result.hasNext());
            Node at = (Node) result.next().get("n");
            assertTrue(at.hasLabel(Label.label("STUB_Event")));
        }));
    }
}
