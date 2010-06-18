/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.impl.interceptor;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.ActivitiException;
import org.activiti.impl.calendar.BusinessCalendarManager;
import org.activiti.impl.el.ExpressionManager;
import org.activiti.impl.job.JobHandlers;
import org.activiti.impl.jobexecutor.JobExecutor;
import org.activiti.impl.msg.MessageSession;
import org.activiti.impl.persistence.PersistenceSession;
import org.activiti.impl.persistence.PersistenceSessionFactory;
import org.activiti.impl.repository.DeployerManager;
import org.activiti.impl.repository.ProcessCache;
import org.activiti.impl.scripting.ScriptingEngines;
import org.activiti.impl.timer.TimerSession;
import org.activiti.impl.tx.TransactionContext;
import org.activiti.impl.variable.VariableTypes;


/**
 * @author Tom Baeyens
 */
public class CommandContext {
  
  private static Logger log = Logger.getLogger(CommandContext.class.getName());

  static ThreadLocal<Stack<CommandContext>> txContextStacks = new ThreadLocal<Stack<CommandContext>>();
  
  Command<?> command;
  Throwable exception = null;
  PersistenceSession persistenceSession;
  MessageSession messageSession;
  TimerSession timerSession;
  CommandContextFactory commandContextFactory;
  TransactionContext transactionContext;

  public CommandContext(Command<?> command, 
                        CommandContextFactory commandContextFactory) {
    this.command = command;
    this.commandContextFactory = commandContextFactory;

    this.transactionContext = commandContextFactory
      .getProcessEngineConfiguration()
      .getTransactionContextFactory()
      .openTransactionContext(this);
  
    this.persistenceSession = commandContextFactory
      .getProcessEngineConfiguration()
      .getPersistenceSessionFactory()
      .openPersistenceSession(this);
    
    this.messageSession = commandContextFactory
    .getProcessEngineConfiguration()
      .getMessageSessionFactory()
      .openMessageSession(this);
    
    this.timerSession = commandContextFactory
      .getProcessEngineConfiguration()
      .getTimerSessionFactory()
      .openTimerSession(this);
  
    getContextStack(true).push(this);
  }

  public void close() {
    // the intention of this method is that all resources are closed properly, even 
    // if exceptions occur in close or flush methods of the sessions or the 
    // transaction context.
    
    try {
      try {
        try {
          try {
            
            if (exception==null) {
              // first flush
              persistenceSession.flush();
              messageSession.flush();
            }
            
          } catch (Throwable exception) {
            exception(exception);
          } finally {
            
            try {
              if (exception==null) {
                transactionContext.commit();
              }
            } catch (Throwable exception) {
              exception(exception);
            }
            
            if (exception!=null) {
              transactionContext.rollback();
            }
          }
        } catch (Throwable exception) {
          exception(exception);
        } finally {

          messageSession.close();

        }
      } catch (Throwable exception) {
        exception(exception);
      } finally {

        persistenceSession.close();

      }
    } catch (Throwable exception) {
      exception(exception);
    } finally {

      getContextStack(true).pop();
    }

    // rethrow the original exception if there was one
    if (exception!=null) {
      if (exception instanceof Error) {
        throw (Error) exception;
      } else if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else {
        throw new ActivitiException("exception while executing command "+command, exception);
      }
    }
  }

  public void exception(Throwable exception) {
    if (this.exception==null) {
      this.exception = exception;
    } else {
      log.log(Level.SEVERE, "exception potentially caused by previous exception", exception);
    }
  }
  
  protected static Stack<CommandContext> getContextStack(boolean isInitializationRequired) {
    Stack<CommandContext> txContextStack = txContextStacks.get();
    if (txContextStack==null && isInitializationRequired) {
      txContextStack = new Stack<CommandContext>();
      txContextStacks.set(txContextStack);
    }
    return txContextStack;
  }
  
  public static CommandContext getCurrent() {
    Stack<CommandContext> contextStack = getContextStack(false);
    if ( (contextStack==null)
         || (contextStack.isEmpty())
       ) {
      return null;
    }
    return contextStack.peek();
  }

  public PersistenceSession getPersistenceSession() {
    return persistenceSession;
  }
  public MessageSession getMessageSession() {
    return messageSession;
  }
  public TimerSession getTimerSession() {
    return timerSession;
  }
  public TransactionContext getTransactionContext() {
    return transactionContext;
  }

  
  public DeployerManager getDeployerManager() {
    return commandContextFactory.getProcessEngineConfiguration().getDeployerManager();
  }

  public ProcessCache getProcessCache() {
    return commandContextFactory.getProcessEngineConfiguration().getProcessCache();
  }

  public ScriptingEngines getScriptingEngines() {
    return commandContextFactory.getProcessEngineConfiguration().getScriptingEngines();
  }

  public VariableTypes getVariableTypes() {
    return commandContextFactory.getProcessEngineConfiguration().getVariableTypes();
  }
  
  public PersistenceSessionFactory getPersistenceSessionFactory() {
    return commandContextFactory.getProcessEngineConfiguration().getPersistenceSessionFactory();
  }
  
  public ExpressionManager getExpressionManager() {
    return commandContextFactory.getProcessEngineConfiguration().getExpressionManager();
  }

  public JobExecutor getJobExecutor() {
    return commandContextFactory.getProcessEngineConfiguration().getJobExecutor();
  }

  public JobHandlers getJobHandlers() {
    return commandContextFactory.getProcessEngineConfiguration().getJobHandlers();
  }

  public BusinessCalendarManager getBusinessCalendarManager() {
    return commandContextFactory.getProcessEngineConfiguration().getBusinessCalendarManager();
  }
}
