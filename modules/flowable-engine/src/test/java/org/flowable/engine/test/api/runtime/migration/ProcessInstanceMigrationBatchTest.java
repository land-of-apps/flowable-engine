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

package org.flowable.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationResult;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationBatchTest extends PluggableFlowableTestCase {

    private ObjectMapper objectMapper;
    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        objectMapper = processEngineConfiguration.getObjectMapper();
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
        deleteProcessInstanceMigrationBatches();
        deleteDeployments();
    }

    @Test
    public void testSimpleBatchMigrationValidationInvalidTargetProcessDefinitionId() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition("UnknownKey", 1);

        //Submit the batch
        try {
            String batchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
            fail("Should not create the batches");
        } catch (FlowableException e) {
            assertTextPresent("no processes deployed with key 'UnknownKey'", e.getMessage());
        }
    }

    @Test
    public void testSimpleBatchMigrationValidationInvalidSourceProcessDefinitionId() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version1ProcessDef.getId());

        //Submit the batch
        try {
            String batchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances("UnknownKey", 2, null);
            fail("Should not create the batches");
        } catch (FlowableException e) {
            assertTextPresent("no processes deployed with key 'UnknownKey'", e.getMessage());
        }
    }

    @Test
    public void testSimpleBatchMigrationValidationMissingMapping() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("MP");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("MP");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        completeTask(task);

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId());

        //Submit the batch
        String validationBatchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        //        ProcessMigrationBatch validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        //        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        ProcessInstanceMigrationResult<List<String>> validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults.isCompleted()).isFalse();

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        //        validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        //        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults).isNotNull();
        assertThat(validationResults.getPartsCount()).isEqualTo(2L);
        assertThat(validationResults.getFailedPartsCount()).isEqualTo(2L);

        List<String> procInstanceIds = validationResults.getFailedParts().stream()
            .map(ProcessInstanceMigrationResult::getProcessInstanceId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        assertThat(procInstanceIds).containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        List<String> validationMessages = validationResults.getFailedParts().stream()
            .map(ProcessInstanceMigrationResult::getResultValue)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        assertThat(validationMessages)
            .containsExactlyInAnyOrder("Process instance (id:'" + processInstance1.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)",
                "Process instance (id:'" + processInstance2.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)");

        //Try batch migrate the process instances
        String migrationBatchId = processInstanceMigrationBuilder.batchMigrateProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        //        ProcessMigrationBatch migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        //        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        ProcessInstanceMigrationResult<String> migrationResult = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);

        //Partial Results
        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).get().isEqualTo(migrationBatchId);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_IN_PROGRESS);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getInProgressPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getCompletedPartsCount).isEqualTo(0L);

        for (ProcessInstanceMigrationResult<String> part : migrationResult.getParts()) {
            assertThat(part).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_IN_PROGRESS);
            assertThat(part.getResultValue()).isEmpty();
            assertThat(part).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(0L);
        }

        //Start async executor to process the batches
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        //        migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        //        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        migrationResult = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);
        assertThat(migrationResult).isNotNull();

        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).get().isEqualTo(migrationBatchId);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_COMPLETED);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getInProgressPartsCount).isEqualTo(0L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getCompletedPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getSuccessfulPartsCount).isEqualTo(0L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getFailedPartsCount).isEqualTo(2L);

        for (ProcessInstanceMigrationResult<String> part : migrationResult.getParts()) {
            assertThat(part).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_COMPLETED);
            assertThat(part).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(0L);
            assertThat(part.getResultStatus()).get().isEqualTo(ProcessInstanceMigrationResult.RESULT_FAILED);
            assertThat(part.getResultValue()).get().isEqualTo("Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent");
        }

        //Confirm no migration happened
        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());

        processInstanceMigrationService.deleteBatchAndResourcesById(validationBatchId);
        processInstanceMigrationService.deleteBatchAndResourcesById(migrationBatchId);
    }

    @Test
    public void testSimpleBatchMigrationValidationMissingMappingPartialAutoMap() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("MP");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("MP");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        completeTask(task);

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId());

        //Submit the batch
        String validationBatchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        //        ProcessMigrationBatch validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        //        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        ProcessInstanceMigrationResult<List<String>> validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults.isCompleted()).isFalse();
        assertThat(validationResults.getPartsCount()).isEqualTo(2L);

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        //        validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        //        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults).isNotNull();
        assertThat(validationResults.getPartsCount()).isEqualTo(2L);

        assertThat(validationResults.getFailedPartsCount()).isEqualTo(1L);

        List<String> procInstanceIds = validationResults.getFailedParts().stream()
            .map(ProcessInstanceMigrationResult::getProcessInstanceId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        assertThat(procInstanceIds).containsExactlyInAnyOrder(processInstance1.getId());

        List<String> validationMessages = validationResults.getFailedParts().stream()
            .map(ProcessInstanceMigrationResult::getResultValue)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        assertThat(validationMessages)
            .containsExactlyInAnyOrder("Process instance (id:'" + processInstance1.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)");

        assertThat(validationResults.getSuccessfulPartsCount()).isEqualTo(1L);
        procInstanceIds = validationResults.getSuccessfulParts().stream()
            .map(ProcessInstanceMigrationResult::getProcessInstanceId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        assertThat(procInstanceIds).containsExactlyInAnyOrder(processInstance2.getId());

        //Migrate the process
        //Try batch migrate the process instances
        String migrationBatchId = processInstanceMigrationBuilder.batchMigrateProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        //        ProcessMigrationBatch migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        //        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        ProcessInstanceMigrationResult<String> migrationResult = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);

        //Partial Results
        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).contains(migrationBatchId);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_IN_PROGRESS);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getInProgressPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getCompletedPartsCount).isEqualTo(0L);

        for (ProcessInstanceMigrationResult<String> part : migrationResult.getParts()) {
            assertThat(part).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_IN_PROGRESS);
            assertThat(part.getResultValue()).isEmpty();
            assertThat(part).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(0L);
        }

        //Start async executor to process the batches
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        //        migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        //        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        migrationResult = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);
        assertThat(migrationResult).isNotNull();

        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).get().isEqualTo(migrationBatchId);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_COMPLETED);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getInProgressPartsCount).isEqualTo(0L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getCompletedPartsCount).isEqualTo(2L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getSuccessfulPartsCount).isEqualTo(1L);
        assertThat(migrationResult).extracting(ProcessInstanceMigrationResult::getFailedPartsCount).isEqualTo(1L);

        for (ProcessInstanceMigrationResult<String> part : migrationResult.getSuccessfulParts()) {
            assertThat(part).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_COMPLETED);
            assertThat(part).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(0L);
            assertThat(part.getResultStatus()).get().isEqualTo(ProcessInstanceMigrationResult.RESULT_SUCCESSFUL);
            assertThat(part.getResultValue()).isEmpty();
        }

        for (ProcessInstanceMigrationResult<String> part : migrationResult.getFailedParts()) {
            assertThat(part).extracting(ProcessInstanceMigrationResult::getStatus).isEqualTo(ProcessInstanceMigrationResult.STATUS_COMPLETED);
            assertThat(part).extracting(ProcessInstanceMigrationResult::getPartsCount).isEqualTo(0L);
            assertThat(part.getResultStatus()).get().isEqualTo(ProcessInstanceMigrationResult.RESULT_FAILED);
            assertThat(part.getResultValue()).get().isEqualTo("Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent");
        }

        //Confirm the migration
        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        //This task migrated
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version2ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());

        processInstanceMigrationService.deleteBatchAndResourcesById(validationBatchId);
        processInstanceMigrationService.deleteBatchAndResourcesById(migrationBatchId);
    }
}
