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
package org.flowable.cmmn.engine.impl.cmd;

import java.util.Date;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.task.api.Task;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class StartProgressTaskCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;

    protected String userId;

    public StartProgressTaskCmd(String taskId, String userId) {
        super(taskId);
        this.userId = userId;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        Clock clock = cmmnEngineConfiguration.getClock();
        Date updateTime = clock.getCurrentTime();
        task.setInProgressStartTime(updateTime);
        task.setInProgressStartedBy(userId);
        task.setState(Task.IN_PROGRESS);
        
        HistoricTaskService historicTaskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
        historicTaskService.recordTaskInfoChange(task, updateTime, cmmnEngineConfiguration);
        
        if (cmmnEngineConfiguration.getHumanTaskStateInterceptor() != null) {
            cmmnEngineConfiguration.getHumanTaskStateInterceptor().handleInProgressStart(task, userId);
        }

        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot start progress on a suspended task";
    }

}
