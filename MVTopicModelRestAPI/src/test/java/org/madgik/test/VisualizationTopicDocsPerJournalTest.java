package org.madgik.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.madgik.dtos.TopicCurationDto;
import org.madgik.dtos.TopicDto;
import org.madgik.dtos.TopicIdDto;
import org.madgik.dtos.VisualizationTopicDocsPerJournalDto;
import org.madgik.services.TopicCurationService;
import org.madgik.services.TopicService;
import org.madgik.services.VisualizationTopicDocsPerJournalService;
import org.madgik.test.config.TestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        SqlScriptsTestExecutionListener.class,
        TransactionalTestExecutionListener.class})
@WebAppConfiguration
@TestPropertySource("/test.properties")
@Transactional(transactionManager = TestConfig.TRANSACTION)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:visualizationTopicDocsPerJournal-inserts.sql"}),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = {"classpath:cleanup.sql"})
})
public class VisualizationTopicDocsPerJournalTest {

    @Autowired
    private WebApplicationContext wac;

    @SuppressWarnings("unused")
    private MockMvc mockMvc;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private VisualizationTopicDocsPerJournalService visualizationTopicDocsPerJournalService;

    @Before
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void test() {
        List<VisualizationTopicDocsPerJournalDto> dtos = visualizationTopicDocsPerJournalService.getAllVisualizationTopicDocsPerJournal();
        Assert.assertNotNull(dtos);
    }

}
