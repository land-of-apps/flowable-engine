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

package org.flowable.engine.migration;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;

/**
 * @author Dennis Federico
 */
public interface ProcessInstanceMigrationManager {

    ProcessInstanceMigrationResult<List<String>> validateMigrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenant, ProcessInstanceMigrationDocument processInstanceMigrationDocument, CommandContext commandContext);

    ProcessInstanceMigrationResult<List<String>> validateMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument, CommandContext commandContext);

    ProcessInstanceMigrationResult<List<String>> validateMigrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument, CommandContext commandContext);

    ProcessMigrationBatchEntity batchValidateMigrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenant, ProcessInstanceMigrationDocument processInstanceMigrationDocument, CommandContext commandContext);

    ProcessMigrationBatchEntity batchValidateMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument, CommandContext commandContext);

    void migrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument document, CommandContext commandContext);

    void migrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext);

    void migrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument document, CommandContext commandContext);

    ProcessMigrationBatchEntity batchMigrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext);

    ProcessMigrationBatchEntity batchMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument document, CommandContext commandContext);
}
