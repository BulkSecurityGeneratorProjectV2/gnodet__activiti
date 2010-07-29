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

package org.activiti.engine.impl.persistence.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.persistence.RepositorySession;
import org.activiti.engine.impl.persistence.repository.Deployer;
import org.activiti.engine.impl.persistence.repository.DeploymentEntity;
import org.activiti.engine.impl.persistence.repository.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.repository.ResourceEntity;
import org.activiti.impl.bytes.ByteArrayImpl;
import org.activiti.impl.definition.ProcessDefinitionImpl;
import org.activiti.impl.interceptor.CommandContext;
import org.activiti.impl.tx.Session;


/**
 * @author Tom Baeyens
 */
public class DbRepositorySession implements Session, RepositorySession {

  protected List<Deployer> deployers = new ArrayList<Deployer>();
  protected Map<String, ProcessDefinitionEntity> processDefinitionCache = new HashMap<String, ProcessDefinitionEntity>(); 
  protected DbSqlSession dbSqlSession;
  
  public DbRepositorySession(List<Deployer> deployers, Map<String, ProcessDefinitionEntity> processDefinitionCache) {
    this.deployers = deployers;
    this.processDefinitionCache = processDefinitionCache;
    this.dbSqlSession = CommandContext
      .getCurrent()
      .getSession(DbSqlSession.class);
  }

  public void close() {
  }

  public void flush() {
  }

  public void deployNew(DeploymentEntity deployment) {
    dbSqlSession.insert(deployment);
    for (ResourceEntity resource: deployment.getResources().values()) {
      resource.setDeploymentId(deployment.getId());
      dbSqlSession.insert(resource);
    }
    for (Deployer deployer: deployers) {
      deployer.deploy(deployment, this, true);
    }
  }

  public void deployExisting(DeploymentEntity deployment) {
    for (Deployer deployer: deployers) {
      deployer.deploy(deployment, this, false);
    }
  }

  public void deleteDeployment(String deploymentId) {
    dbSqlSession.delete("deleteProcessDefinitionsByDeploymentId", deploymentId);
    dbSqlSession.delete("deleteResourcesByDeploymentId", deploymentId);
    dbSqlSession.delete("deleteDeploymentById", deploymentId);
  }

  public ResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("deploymentId", deploymentId);
    params.put("resourceName", resourceName);
    return (ResourceEntity) dbSqlSession.selectOne("selectResourceByDeploymentIdAndResourceName", params);
  }

  @SuppressWarnings("unchecked")
  public List<DeploymentEntity> findDeployments() {
    return (List<DeploymentEntity>) dbSqlSession.selectList("selectDeployments");
  };

  public DeploymentEntity findLatestDeploymentByName(String deploymentName) {
    return (DeploymentEntity) dbSqlSession.selectOne("selectLatestDeploymentByName", deploymentName);
  }

  public void insertProcessDefinition(ProcessDefinitionImpl processDefinition) {
    dbSqlSession.insert(processDefinition);
  }

  public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKey(String deploymentId, String processDefinitionKey) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("deploymentId", deploymentId);
    parameters.put("processDefinitionKey", processDefinitionKey);
    return (ProcessDefinitionEntity) dbSqlSession.selectOne("selectProcessDefinitionByDeploymentAndKey", parameters);
  }

  @SuppressWarnings("unchecked")
  public List<ProcessDefinitionEntity> findUndeployedProcessDefinitionsByDeploymentId(String deploymentId) {
    return dbSqlSession.selectList("selectProcessDefinitionsByDeploymentId", deploymentId);
  }
}