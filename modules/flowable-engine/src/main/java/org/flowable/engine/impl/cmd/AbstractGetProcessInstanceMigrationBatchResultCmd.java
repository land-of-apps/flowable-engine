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

package org.flowable.engine.impl.cmd;

import java.io.IOException;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.jobexecutor.AbstractProcessInstanceMigrationJobHandler;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationResultImpl;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationResult;
import org.flowable.engine.runtime.ProcessMigrationBatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dennis Federico
 */
public abstract class AbstractGetProcessInstanceMigrationBatchResultCmd<T> implements Command<ProcessInstanceMigrationResult<T>> {

    public String batchId;

    public AbstractGetProcessInstanceMigrationBatchResultCmd(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public ProcessInstanceMigrationResult<T> execute(CommandContext commandContext) {

        ProcessMigrationBatchEntityManager batchManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessMigrationBatchEntityManager();
        ProcessMigrationBatchEntity batch = batchManager.findById(batchId);

        if (batch != null) {
            ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();
            ProcessInstanceMigrationResultImpl<T> result = convertFromBatch(batch, objectMapper);
            if (batch.getBatchChildren() != null) {
                batch.getBatchChildren().forEach(child -> result.addResultPart(convertFromBatch(child, objectMapper)));
            }
            return result;
        }
        return null;
    }

    protected ProcessInstanceMigrationResultImpl<T> convertFromBatch(ProcessMigrationBatch batch, ObjectMapper objectMapper) {
        ProcessInstanceMigrationResultImpl<T> result = new ProcessInstanceMigrationResultImpl<>();

        result.setBatchId(batch.getId());
        result.setProcessInstanceId(batch.getProcessInstanceId());

        //getCompleteTime traverses the children (if any)
        if (batch.getCompleteTime() != null) {
            result.setStatus(ProcessInstanceMigrationResult.STATUS_COMPLETED);
        }
        if (batch.getResult() != null) {
            try {
                JsonNode resultNode = objectMapper.readTree(batch.getResult());
                String resultStatus = null;
                if (resultNode.has(AbstractProcessInstanceMigrationJobHandler.BATCH_RESULT_STATUS_LABEL)) {
                    resultStatus = resultNode.get(AbstractProcessInstanceMigrationJobHandler.BATCH_RESULT_STATUS_LABEL).asText();
                }

                T resultValue = null;
                if (resultNode.has(AbstractProcessInstanceMigrationJobHandler.BATCH_RESULT_VALUE_LABEL)) {
                    resultValue = getResultFromBatch(batch, resultNode.get(AbstractProcessInstanceMigrationJobHandler.BATCH_RESULT_VALUE_LABEL), objectMapper);
                }
                result.setResult(resultStatus, resultValue);
            } catch (IOException e) {

            }
        }
        return result;
    }

    protected abstract T getResultFromBatch(ProcessMigrationBatch batch, JsonNode jsonNode, ObjectMapper objectMapper);
}
