/**
 * 
 */
package org.activiti.test.jobexecutor;

import org.activiti.impl.ProcessEngineImpl;
import org.activiti.impl.cmd.DeleteJobsCmd;
import org.activiti.impl.interceptor.Command;
import org.activiti.impl.interceptor.CommandContext;
import org.activiti.impl.interceptor.CommandExecutor;
import org.activiti.impl.job.MessageImpl;
import org.activiti.test.ActivitiTestCase;


/**
 * @author tombaeyens
 *
 */
public class JobExecutorCmdExceptionTest extends ActivitiTestCase {

  protected TweetExceptionHandler tweetExceptionHandler = new TweetExceptionHandler();
  
  protected void setUp() throws Exception {
    super.setUp();
    
    ProcessEngineImpl processEngineImpl = (ProcessEngineImpl)processEngine;
    processEngineImpl
      .getProcessEngineConfiguration()
      .getJobCommands()
      .addJobHandler(tweetExceptionHandler);
  }

  protected void tearDown() throws Exception {
    ProcessEngineImpl processEngineImpl = (ProcessEngineImpl)processEngine;
    processEngineImpl
      .getProcessEngineConfiguration()
      .getJobCommands()
      .removeJobHandler(tweetExceptionHandler);

    super.tearDown();
  }

  public void testJobCommandsWith2Exceptions() {
    ProcessEngineImpl processEngineImpl = (ProcessEngineImpl)processEngine;
    CommandExecutor commandExecutor = processEngineImpl.getProcessEngineConfiguration().getCommandExecutor();
    commandExecutor.execute(new Command<String>() {
      public String execute(CommandContext commandContext) {
        MessageImpl message = createTweetExceptionMessage();
        commandContext.getMessageSession().send(message);
        return message.getId();
      }
    });

    waitForJobExecutorToProcessAllJobs(8000, 250);
  }

  public void testJobCommandsWith3Exceptions() {
    tweetExceptionHandler.setExceptionsRemaining(3);
    
    ProcessEngineImpl processEngineImpl = (ProcessEngineImpl)processEngine;
    CommandExecutor commandExecutor = processEngineImpl.getProcessEngineConfiguration().getCommandExecutor();
    String jobId = commandExecutor.execute(new Command<String>() {
      public String execute(CommandContext commandContext) {
        MessageImpl message = createTweetExceptionMessage();
        commandContext.getMessageSession().send(message);
        return message.getId();
      }
    });

    waitForJobExecutorToProcessAllJobs(8000, 250);
    
    // TODO check if there is a failed job in the DLQ

    commandExecutor.execute(new DeleteJobsCmd(jobId));
  }

  protected MessageImpl createTweetExceptionMessage() {
    MessageImpl message = new MessageImpl();
    message.setJobHandlerType("tweet-exception");
    return message;
  }
}
