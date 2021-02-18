package MineAChunkApp;

import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.MCVersion;
import kaptainwutax.seedutils.mc.pos.BPos;
import kaptainwutax.seedutils.mc.pos.CPos;
import kaptainwutax.seedutils.util.math.DistanceMetric;
import kaptainwutax.seedutils.util.math.Vec3i;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

class StructureSeedThread implements Runnable{
    private int offset, totalThreads;
    //Check for intersections from points (x1,z1) and (x2,z2) at angles theta1, theta2 within scale blocks.
    //If they intersect,store in x,z

    public StructureSeedThread(int offset, int totalThreads){
        this.offset = offset;
        this.totalThreads = totalThreads;
    }

    public void run(){
        System.out.println("Started StructureSeedThread " + (this.offset + 1) + "/" + this.totalThreads + " on seed #: "+(RavineFilter.START_STRUCTURE_SEED + this.offset));
        RavineFilter.filterStructureSeeds(this.offset, this.totalThreads);
        //At this point, ctrl-c once you've got enough seeds (you think) and paste the output of the program into allSeeds up top. Running it again will generate
        //actual seeds and coordinates of intersection.
    }
}

public class Main {
    public static final String OPTIONS_PATHNAME = "options.txt";
    public static final String STRUCTURE_SEEDS_PATHNAME = "output-structureSeeds.txt";
    public static final String WORLD_SEEDS_PATHNAME = "output-worldSeeds.txt";

    public static int THREAD_COUNT = 1;

    public static void createThread(int t) throws InterruptedException {
        if (t < THREAD_COUNT - 1) {
            Runnable structureSeedThread = new StructureSeedThread(t, THREAD_COUNT);
            Thread thread = new Thread(structureSeedThread);
            thread.start();
            t++;
            createThread(t);
            thread.join();
        } else {
            Runnable structureSeedThread = new StructureSeedThread(t, THREAD_COUNT);
            Thread thread = new Thread(structureSeedThread);
            thread.start();
            thread.join();
        }
    }

    public static void main (String[] args) throws InterruptedException {
        try {
            File file = new File(OPTIONS_PATHNAME);
            Scanner reader = new Scanner(file);
            RavineFilter.START_STRUCTURE_SEED = Long.parseLong(reader.nextLine());
            RavineFilter.MAX_STRUCTURE_SEEDS = Integer.parseInt(reader.nextLine());
            THREAD_COUNT = Integer.parseInt(reader.nextLine());
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        RavineFilter.viableSeeds.clear();

        int t = 0;
        createThread(t);

        try {
            FileWriter writer = new FileWriter(STRUCTURE_SEEDS_PATHNAME);
            for (Map.Entry mElement : RavineFilter.viableSeeds.entrySet()) {
                long structureSeed = (long) mElement.getKey();
                BPos intersect = (BPos) mElement.getValue();
                writer.write(structureSeed + " x: " + intersect.getX() + " z: " + intersect.getZ() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}