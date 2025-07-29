package org.openpnp.api.services;

import org.openpnp.api.exceptions.JobNotLoadedException;
import org.openpnp.api.exceptions.MachineNotEnabledException;
import org.openpnp.api.models.job.JobInfo;
import org.openpnp.api.models.job.JobStatus;
import org.openpnp.api.models.job.PlacementInfo;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.JobPanel;
import org.openpnp.model.*;
import org.openpnp.spi.Machine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с заданиями OpenPnP
 */
public class JobService {

    /**
     * Получить информацию о текущем задании
     */
    public JobInfo getCurrentJobInfo() throws Exception {
        JobPanel jobPanel = getJobPanel();
        Job job = jobPanel.getJob();

        if (job == null) {
            throw new JobNotLoadedException();
        }

        JobInfo jobInfo = new JobInfo();
        // Job не имеет getName(), используем имя файла или корневой панели
        if (job.getFile() != null) {
            jobInfo.setName(job.getFile().getName());
        } else {
            jobInfo.setName(job.getRootPanelLocation().getPanel().getName());
        }

        if (job.getFile() != null) {
            jobInfo.setFile(job.getFile().getAbsolutePath());
        }

        jobInfo.setDirty(job.isDirty());

        // Получаем размещения из всех BoardLocation
        List<PlacementInfo> placements = new ArrayList<>();
        List<BoardLocation> boardLocations = job.getBoardLocations();

        for (BoardLocation boardLocation : boardLocations) {
            Board board = boardLocation.getBoard();
            if (board != null) {
                for (Placement placement : board.getPlacements()) {
                    PlacementInfo placementInfo = new PlacementInfo();
                    placementInfo.setId(placement.getId());
                    placementInfo.setBoardName(board.getName());
                    placementInfo.setPartId(placement.getPart() != null ? placement.getPart().getId() : null);
                    placementInfo.setType(PlacementInfo.Type.valueOf(placement.getType().toString()));
                    placementInfo.setSide(placement.getSide() == Placement.Side.Top ? PlacementInfo.Side.TOP
                            : PlacementInfo.Side.BOTTOM);
                    placementInfo.setX(placement.getLocation().getX());
                    placementInfo.setY(placement.getLocation().getY());
                    placementInfo.setZ(placement.getLocation().getZ());
                    placementInfo.setRotation(placement.getLocation().getRotation());
                    placementInfo.setEnabled(placement.isEnabled());
                    placementInfo.setPlaced(job.retrievePlacedStatus(boardLocation, placement.getId()));
                    placementInfo.setComment(placement.getComments());

                    placements.add(placementInfo);
                }
            }
        }

        jobInfo.setPlacements(placements);
        jobInfo.setTotalPlacements(placements.size());

        // Подсчитываем завершенные размещения
        int completedCount = 0;
        for (PlacementInfo placement : placements) {
            if (placement.isPlaced()) {
                completedCount++;
            }
        }
        jobInfo.setCompletedPlacements(completedCount);

        return jobInfo;
    }

    /**
     * Получить статус выполнения задания
     */
    public JobStatus getJobStatus() throws Exception {
        JobPanel jobPanel = getJobPanel();
        Job job = jobPanel.getJob();

        if (job == null) {
            throw new JobNotLoadedException();
        }

        JobStatus jobStatus = new JobStatus();

        // Определяем состояние задания через рефлексию к private полю state
        try {
            java.lang.reflect.Field stateField = JobPanel.class.getDeclaredField("state");
            stateField.setAccessible(true);
            Object state = stateField.get(jobPanel);

            switch (state.toString()) {
                case "Stopped":
                    jobStatus.setState(JobStatus.State.STOPPED);
                    break;
                case "Running":
                    jobStatus.setState(JobStatus.State.RUNNING);
                    break;
                case "Paused":
                    jobStatus.setState(JobStatus.State.PAUSED);
                    break;
                case "Pausing":
                    jobStatus.setState(JobStatus.State.PAUSING);
                    break;
                case "Stopping":
                    jobStatus.setState(JobStatus.State.STOPPING);
                    break;
                default:
                    jobStatus.setState(JobStatus.State.STOPPED);
            }
        } catch (Exception e) {
            // Если не удается получить состояние, считаем что остановлено
            jobStatus.setState(JobStatus.State.STOPPED);
        }

        // Получаем общее количество размещений из всех BoardLocation
        List<BoardLocation> boardLocations = job.getBoardLocations();
        int totalPlacements = 0;
        int completedCount = 0;

        for (BoardLocation boardLocation : boardLocations) {
            Board board = boardLocation.getBoard();
            if (board != null) {
                for (Placement placement : board.getPlacements()) {
                    if (placement.getType() == Placement.Type.Placement && placement.isEnabled()) {
                        totalPlacements++;
                        if (job.retrievePlacedStatus(boardLocation, placement.getId())) {
                            completedCount++;
                        }
                    }
                }
            }
        }

        jobStatus.setTotalSteps(totalPlacements);
        jobStatus.setCompletedSteps(completedCount);
        jobStatus.setCurrentStep("Размещение компонентов");

        return jobStatus;
    }

    /**
     * Загрузить задание из файла
     */
    public void loadJob(String filePath) throws Exception {
        File jobFile = new File(filePath);
        if (!jobFile.exists()) {
            throw new Exception("Файл задания не найден: " + filePath);
        }

        Configuration config = Configuration.get();
        Job job = config.loadJob(jobFile);

        JobPanel jobPanel = getJobPanel();
        jobPanel.setJob(job);
    }

    /**
     * Запустить выполнение задания
     */
    public void startJob() throws Exception {
        Machine machine = Configuration.get().getMachine();
        if (machine == null || !machine.isEnabled()) {
            throw new MachineNotEnabledException();
        }

        JobPanel jobPanel = getJobPanel();
        Job job = jobPanel.getJob();

        if (job == null) {
            throw new JobNotLoadedException();
        }

        // Запускаем задание через метод jobStart()
        jobPanel.jobStart();
    }

    /**
     * Приостановить выполнение задания
     */
    public void pauseJob() throws Exception {
        JobPanel jobPanel = getJobPanel();
        Job job = jobPanel.getJob();

        if (job == null) {
            throw new JobNotLoadedException();
        }

        // Устанавливаем состояние паузы через рефлексию
        try {
            java.lang.reflect.Method setStateMethod = JobPanel.class.getDeclaredMethod("setState",
                    Class.forName("org.openpnp.gui.JobPanel$State"));
            setStateMethod.setAccessible(true);

            // Получаем enum значение State.Pausing
            Class<?> stateClass = Class.forName("org.openpnp.gui.JobPanel$State");
            Object pausingState = Enum.valueOf((Class<Enum>) stateClass, "Pausing");
            setStateMethod.invoke(jobPanel, pausingState);
        } catch (Exception e) {
            throw new Exception("Не удалось приостановить задание: " + e.getMessage());
        }
    }

    /**
     * Остановить выполнение задания
     */
    public void stopJob() throws Exception {
        JobPanel jobPanel = getJobPanel();
        Job job = jobPanel.getJob();

        if (job == null) {
            throw new JobNotLoadedException();
        }

        // Устанавливаем состояние остановки через рефлексию
        try {
            java.lang.reflect.Method setStateMethod = JobPanel.class.getDeclaredMethod("setState",
                    Class.forName("org.openpnp.gui.JobPanel$State"));
            setStateMethod.setAccessible(true);

            // Получаем enum значение State.Stopping
            Class<?> stateClass = Class.forName("org.openpnp.gui.JobPanel$State");
            Object stoppingState = Enum.valueOf((Class<Enum>) stateClass, "Stopping");
            setStateMethod.invoke(jobPanel, stoppingState);
        } catch (Exception e) {
            throw new Exception("Не удалось остановить задание: " + e.getMessage());
        }
    }

    /**
     * Получить панель заданий из MainFrame
     */
    private JobPanel getJobPanel() throws Exception {
        MainFrame mainFrame = MainFrame.get();
        if (mainFrame == null) {
            throw new Exception("MainFrame не инициализирован");
        }

        return mainFrame.getJobTab();
    }
}