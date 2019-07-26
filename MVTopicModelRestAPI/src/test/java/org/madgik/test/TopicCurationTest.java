package org.madgik.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.madgik.dtos.TopicCurationDto;
import org.madgik.dtos.TopicCurationIdDto;
import org.madgik.dtos.TopicDto;
import org.madgik.dtos.TopicIdDto;
import org.madgik.persistence.entities.Topic;
import org.madgik.persistence.entities.TopicId;
import org.madgik.services.TopicCurationService;
import org.madgik.services.TopicService;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        SqlScriptsTestExecutionListener.class,
        TransactionalTestExecutionListener.class})
@WebAppConfiguration
@TestPropertySource("/test.properties")
@Transactional(transactionManager = TestConfig.TRANSACTION)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:topiccuration-inserts.sql"}),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = {"classpath:cleanup.sql"})
})

public class TopicCurationTest {

    @Autowired
    private WebApplicationContext wac;

    @SuppressWarnings("unused")
    private MockMvc mockMvc;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TopicCurationService topicCurationService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TopicService topicService;

    @Before
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void testSaveTopicCuration() {
        TopicDto topic = new TopicDto();
        TopicIdDto topicIdDto = new TopicIdDto(3, "abcde");
        topic.setTopicId(topicIdDto);
        topic.setTitle("title3");

        topicService.createNewTopic(topic);

        TopicCurationDto topicCurationDto = new TopicCurationDto();
        topicCurationDto.setTopicId(3);
        topicCurationDto.setExperimentId("abcde");
        topicCurationDto.setTopic(topic);
        topicCurationDto.setCuratedDescription("description3");
        TopicCurationDto savedDto = topicCurationService.createTopicCuration(topicCurationDto);
//        Assert.assertEquals(topicCurationDto.getCuratedDescription(), savedDto.getCuratedDescription());
    }

    @Test
    public void testGetTopicCurationByCompositeId() {
        TopicCurationDto topicCurationDto = topicCurationService.getTopicCurationByTopicIdAndExperimentId(1, "abc");
//        Assert.assertNotNull(topicCurationDto);
//        Assert.assertEquals("description", topicCurationDto.getCuratedDescription());
    }

}
