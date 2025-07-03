# Robust Communication through Collective Adaptive Relay Schemes for Maritime Vessels
#### Experiments for paper _"Robust Communication through Collective Adaptive Relay Schemes for Maritime Vessels"_ submitted to [ACSOS 2025](https://conf.researchr.org/home/acsos-2025) main track

#### Authors

**Martina Baiardi** (m.baiardi@unibo.it),
**Danilo Pianini** (danilo.pianini@unibo.it)

Department of Computer Science and Engineering
Alma Mater Studiorum --- Università di Bologna - Cesena, Italy

**Ghassan Al-Falouji** (gaf@informatik.uni-kiel.de),
**Sven Tomforde** (st@informatik.uni-kiel.de)

Department of Computer Science
University of Kiel --- Kiel, Germany

### Abstract

Maritime communication networks face unique challenges due to the dynamic and sparse distribution of vessels, variable environmental conditions, and heterogeneous technological constraints. 
With the increasing trend toward autonomy in maritime operations, these challenges become more pronounced.

Modern maritime navigation systems integrate numerous high-bandwidth sensors—including cameras and LiDAR—to enhance environmental perception, whose exploitation generates increased data rate demand. 
The increase is in contrast to traditional ship communication systems, which provide data rates in the order of kilobits per second. 

This work proposes robust, multi-mean, collective adaptive software infrastructures to resiliently improve data collection by relaying data streams across multiple vessels. 
In particular, we introduce a method to form dynamic clusters of vessels whose information is summarised and then transmitted, raising the probability that the information reaches its destination. 
We validate our approach through simulation and show that the proposed clustering mechanism is capable of scaling up as new vessels are equipped with improved communication technologies.

### Goal and Scope

The goal of this experiment is to evaluate the performance of different communication strategies for maritime vessels in the Kiel area.
We evaluate the performance of the communication strategies in terms of data rate,
i.e., the amount of data that can be communicated.

The communication algorithms compared in this experiment are:
- **Direct Communication**: vessels communicate directly with shore stations, which is the current standard communication method. If vessels are out of range of communication with shore stations, they rely on satellite communication.
- **Distance-based Multi-Relay Communication (Dist-MR)**: vessels communicate relaying messages to each other to improve communication range. The relay is chosen based on the distance between vessels, i.e., the closest vessel to the sender is chosen as a relay.
- **Data Rate-based Multi-Relay Communication (DR-MR)**: vessels communicate relaying messages to each other to improve communication range. The relay is chosen based on the data rate, i.e., the vessel with which the data rate is highest is chosen as a relay.
- **Collective Summarization Clusters (CSC)**: Like Dist-MR but the vessels are grouped into clusters, and each cluster has a leader that collects the messages from the other vessels in the cluster and sends them to the nearest relay, which can be a shore station or another vessel in a cluster.

### Technologies adopted for the experiment

- The experiment is implemented in [Kotlin](https://kotlinlang.org/)
- The experiment is built using [Gradle](https://gradle.org/).
- Vessels are programmed with [Collektive](https://github.com/Collektive/collektive),
an open-source framework for modelling Aggregate Systems.
- The simulation is performed using [Alchemist](https://alchemist.github.io/).

This artifact is generated, validated, and published with a GitHub Actions CI/CD pipeline,
its configuration is consultable in the file `.github/workflows/build-and-deploy.yml`.
After each commit on `main` branch, the **automatic** process performs, in order:
1. Checkout of latest code changes
2. Build of the source code
3. Static checks of source code (linters, class/methods documentation)
4. Execution of Unit tests
5. Execution the artifact for a small amount of time, to verify it starts succesfully.
6. Generation of the charts
7. Computation of the release version based on [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/)
and performs the release on GitHub, including the generated charts. The configuration for this process is in `release.config.js` file.
8. Build of docker images and publish on DockerHub

This process ensures the reproducibility at every addition into the code base.

On top of this process,
dependencies are automatically updated using [Renovate](https://docs.renovatebot.com/) (configured in `renovate.json` file),
and automatically tested before their inclusion in the `main` branch.

### Project structure
Relevant files and folders for the experiment relevance in this project are:
```md
experiment-2025-acsos-ship-clustered-comm
├── data/...                            # Data from previously executed simulations to generate charts
├── docker
│   ├── charts/Dockerfile               # Dockerfile that generated the charts
│   └── sim/Dockerfile                  # Dockerfile executing the simulation
├── effects/...                         # Graphic effects for simulation on GUI
├── src
│   ├── main
│   │   ├── kotlin/it/unibo/alchemist/...                     # Alchemist simulator extensions
│   │   ├── kotlin/it/unibo/clustered/seaborn/comm/...        # Collektive source code
│   │   ├── kotlin/it/unibo/util/...                          # Utilities for AIS and GPX parsing
│   │   ├── resources
│   │   │   ├── ais-sample/...          # Example of AIS packages
│   │   │   ├── maps/...                # Geographical info. for Kiel area (geojson and pbf formats)
│   │   │   └── navigation-routes/...   # GPX Traces of vessels extracted from AIS data and anonimised 
│   │   └── yaml/simulation.yml         # Alchemist simulation configuration file
│   └── test/...                        # Unit tests for AIS->GPX and GEOJSON parsing  
├── docker-compose.yml                  
└── process.py                          # Python script to generate charts from data/*
```

### Experiment configuration

The experiment simulates vessels movements taken from a six-hour time window from August 18, 2022, from 4:00 to 10:00 Universal Time Coordinated (UTC) (6:00 to 12:00 Central European Summer Time (CEST)—Kiel local time).

We assume each vessel to be equipped with:
- a VHF Automatic Packet Reporting System (APRS) device and a LoRaWAN class-C (bidirectional communication) device for long-range communication;
- a 5G consumer-grade module (similar to the ones used in mobile phones); and
- a Wi-Fi-direct capable device.

Furthermore, we assume that a vessel can be equipped with
a probability 5G cellular tower _p5G_. 

We use openly available data to locate the positions of the [land stations](https://archive.is/QNhyS).
Similarly, we use openly available data to understand where the [existing 5G infrastructure](https://archive.is/fTMSV) is located and their specific technology.


We assume that all ships intend to communicate a data
stream d = 3 Mbit/s to the land station, approximately
equivalent to the bitrate of a compressed 720p video stream.

We simulation is performed with two free variables:
- _p5G_: Controlling the probability of a vessel being equipped with a 5G tower. We let _p5G_ range in `[0, 0.01, 0.02, 0.05, 0.1, 0.5, 1]`. 
- _f_: Controlling the aggregate program round frequency. We let _f_ range in `[1 / 60, 0.1, 0.2]`Hz.

Every experiment is performed 100 times, each time with a different seed.

The simulation configuration can be found and changed in the `src/main/resources/yaml/simulation.yml` file.

### AIS raw files data processing

The information about vessels positions and movements is extracted from the [Automatic Identification System (AIS)](https://en.wikipedia.org/wiki/Automatic_identification_system) data,
which is the current maritime communication system that allows vessels to exchange information about their position, speed, heading, and other information towards shore stations.
The AIS raw data were collected in the interested area, and then processed to extract the vessels' GPX traces needed for the experiment.

Since the AIS we relied on is not open and privacy concerns apply, we instead share anonymised GPX traces (MMSI numbers are not tracked), which can be found in the `src/main/resources/navigation-routes` folder.
However, we included the AIS conversion programme in the open source release to simplify repurposing of the experiment in other contexts, which is consultable in `src/main/kotlin/it/unibo/util/gpx/ParseRawNavigationData.kt`.

## Getting started

The experiment can be run in three different ways:
1. [Full-Batch-Run] the batched execution of the all 100 seeds of simulation, which generates raw data. This is the most time-consuming step, which produces the results available inside `data` folder. Executing this step requires approximately one week on a machine with 750GiB of RAM and 96 CPU cores. 
2. [Graphical-Run] the simulation can be executed in a graphical mode, which allows to visualise the simulation in real-time. This is useful for debugging and understanding the simulation dynamics, but it is not suitable for generating results, since it runs only a single seed of the simulation and default value for the free variables.
3. [Chart-Generation] the charts can be generated from the data already provided in `data` folder. This is a fast step, which produces the charts inside `charts` folder.

**Note:** There is no hardware requirement for running the experiment, the simulation will use the available hardware resources. Still, it is recommended to run the experiment on a computer with at least 16 GB of RAM and 8 CPU cores. 

### [Full-Batch-Run] Reproduce the entire experiment
**WARNING**: re-running the whole experiment may take a very long time on a normal computer.

#### Reproduce with containers (recommended)
1. Install **Docker** and **docker-compose**
2. Run `docker-compose up`
3. The charts will be available in the `charts` folder.

#### Reproduce natively
1. Install a Gradle-compatible version of [**Java**](https://docs.oracle.com/en/java/javase/21/install/overview-jdk-installation.html).
  Use the [Gradle/Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
  to learn which is the compatible version range.
  The Version of Gradle used in this experiment can be found in the `gradle-wrapper.properties` file
  located in the `gradle/wrapper` folder.
2. Install the version of Python indicated in `.python-version` (or use `pyenv`).
3. Launch either:
    - `./gradlew runAllBatch` on Linux, MacOS, or Windows if a bash-compatible shell is available;
    - `gradlew.bat runAllBatch` on Windows cmd or Powershell;
4. Once the experiment is finished, the results will be available in the `data` folder. Then run:
    - `python -m venv venv`
    - `source venv/bin/activate`
    - `pip install --upgrade pip`
    - `pip install -r requirements.txt`
    - `python process.py`
5. The charts will be available in the `charts` folder.


### [Graphical-Run] Run Single Graphical Experiment
1. Install a Gradle-compatible version of [**Java**](https://docs.oracle.com/en/java/javase/21/install/overview-jdk-installation.html).
  Use the [Gradle/Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
  to learn which is the compatible version range.
  The Version of Gradle used in this experiment can be found in the `gradle-wrapper.properties` file
  located in the `gradle/wrapper` folder.
2. Launch either:
    - `./gradlew runSimulationGraphic` on Linux, MacOS, or Windows if a bash-compatible shell is available;
    - `gradlew.bat runSimulationGraphic` on Windows cmd or Powershell;

The simulation will start automatically, but you are free to pause/resume it by pressing `P` on keyboard.

The graphical execution launches the simulation with default values for the free variables and the random seed,
this can behaviour can be manually customized by changing the configuration in `src/main/yaml/simulation.yml`.

Currently default values for variables are: 
- _p5G_: 0.02
- _f_: 0.2
- _seed_: 0.0

To make changes to existing experiments and explore/reuse,
we recommend to use the **IntelliJ Idea IDE**.
Opening the project in IntelliJ Idea will automatically import the project, download the dependencies,
and allow for a smooth development experience.

### [Chart-Generation] Regenerate the charts

We keep a copy of the data in this repository,
so that the charts can be regenerated without having to run the experiment again.
To regenerate the charts, run `docker compose run --no-deps charts`.
Alternatively, follow the steps or the "reproduce natively" section,
starting from step 4.

The charts will be available in the `charts` folder.

