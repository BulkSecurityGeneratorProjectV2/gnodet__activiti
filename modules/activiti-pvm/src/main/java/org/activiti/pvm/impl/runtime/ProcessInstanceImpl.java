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

package org.activiti.pvm.impl.runtime;

import java.util.ArrayList;
import java.util.List;

import org.activiti.pvm.impl.process.ProcessDefinitionImpl;
import org.activiti.pvm.runtime.PvmProcessInstance;


/**
 * @author Tom Baeyens
 */
public class ProcessInstanceImpl extends ScopeInstanceImpl implements PvmProcessInstance {
  
  public ProcessInstanceImpl(ProcessDefinitionImpl processDefinition) {
    super(processDefinition, processDefinition);
  }
  
  public void start() {
    new ExecutionContextImpl().startProcessInstance(this);
  }

  public List<String> findActivityIds() {
    List<String> activityIds = new ArrayList<String>();
    collectActivityIds(activityIds);
    return activityIds;
  }
}
