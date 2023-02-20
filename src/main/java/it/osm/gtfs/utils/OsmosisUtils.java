package it.osm.gtfs.utils;

import org.openstreetmap.osmosis.core.TaskRegistrar;
import org.openstreetmap.osmosis.core.pipeline.common.Pipeline;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class OsmosisUtils {
    public static void checkProcessOutput(Pipeline process) {
        if (process != null) {
            process.waitForCompletion();
        }
    }

    public static Pipeline runOsmosisSort(File input, File output) {
        TaskRegistrar taskRegistrar = new TaskRegistrar();
        taskRegistrar.initialize(new ArrayList<>());

        List<TaskConfiguration> taskInfoList = new ArrayList<>();
        taskInfoList.add(new TaskConfiguration("1", "read-xml", new HashMap<>(), new HashMap<>(), input.getAbsolutePath()));
        taskInfoList.add(new TaskConfiguration("2", "sort", new HashMap<>(), new HashMap<>(), null));
        taskInfoList.add(new TaskConfiguration("3", "write-xml", new HashMap<>(), new HashMap<>(), output.getAbsolutePath()));

        Pipeline pipeline = new Pipeline(taskRegistrar.getFactoryRegister());
        pipeline.prepare(taskInfoList);
        pipeline.execute();
        return pipeline;
    }

    public static Pipeline runOsmosisMerge(Collection<File> input, File output) {
        TaskRegistrar taskRegistrar = new TaskRegistrar();
        taskRegistrar.initialize(new ArrayList<>());

        List<TaskConfiguration> taskInfoList = new ArrayList<>();
        for (File file : input) {
            taskInfoList.add(new TaskConfiguration("r" + file.getName(), "read-xml", new HashMap<>(), new HashMap<>(), file.getAbsolutePath()));
        }

        for (int i = 1; i < input.size(); i++) {
            taskInfoList.add(new TaskConfiguration("m" + i, "merge", new HashMap<>(), new HashMap<>(), null));
        }

        taskInfoList.add(new TaskConfiguration("w", "write-xml", new HashMap<>(), new HashMap<>(), output.getAbsolutePath()));

        Pipeline pipeline = new Pipeline(taskRegistrar.getFactoryRegister());
        pipeline.prepare(taskInfoList);
        pipeline.execute();
        return pipeline;
    }

    public static Pipeline runOsmosisUnusedWaysNodes(File input, File output) {
        TaskRegistrar taskRegistrar = new TaskRegistrar();
        taskRegistrar.initialize(new ArrayList<>());

        List<TaskConfiguration> taskInfoList = new ArrayList<>();
        taskInfoList.add(new TaskConfiguration("1", "read-xml", new HashMap<>(), new HashMap<>(), input.getAbsolutePath()));
        taskInfoList.add(new TaskConfiguration("2", "used-way", new HashMap<>(), new HashMap<>(), null));
        taskInfoList.add(new TaskConfiguration("3", "used-node", new HashMap<>(), new HashMap<>(), null));
        taskInfoList.add(new TaskConfiguration("4", "write-xml", new HashMap<>(), new HashMap<>(), output.getAbsolutePath()));

        Pipeline pipeline = new Pipeline(taskRegistrar.getFactoryRegister());
        pipeline.prepare(taskInfoList);
        pipeline.execute();
        return pipeline;
    }

}
