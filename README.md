# Experiments for Robust Communication through Collective Adaptive Relay Schemes for Maritime Vessels
### Companion artifact for paper submitted to [ACSOS 2025](https://conf.researchr.org/home/acsos-2025) main track

## Getting started

The experiment is composed of two parts:
1. [Full-Run] the execution of simulation, which generates raw data
2. [Quick-Run] the manipulation of the raw data to generate charts

## [Full-Run] Reproduce the entire experiment (1 + 2)
**WARNING**: re-running the whole experiment may take a very long time on a normal computer.

### Reproduce with containers (recommended)

1. Install **Docker** and **docker-compose**
2. Run `docker-compose up`
3. The charts will be available in the `charts` folder.

### Reproduce natively
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

## [Quick-Run] Regenerate the charts (2)

We keep a copy of the data in this repository,
so that the charts can be regenerated without having to run the experiment again.
To regenerate the charts, run `docker compose run --no-deps charts`.
Alternatively, follow the steps or the "reproduce natively" section,
starting from step 4.

## Artifact Description



### Reproducibility

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

### Code Structure

Relevant files of this artifact are briefly described here:
```md
experiment-2025-acsos-ship-clustered-comm
├── data/...
├── docker
│   ├── charts/Dockerfile
│   └── sim/Dockerfile
├── effects/...
├── src
│   ├── main
│   │   ├── kotlin/it/unibo/...
│   │   ├── resources
│   │   │   ├── ais-sample/...
│   │   │   ├── maps/...
│   │   │   └── navigation-routes/...
│   │   └── yaml/simulation.yml
│   └── test/...
├── docker-compose.yml
└── process.py                          # Python script to generate charts from data/*
```

Follow the instructions for reproducing the entire experiment natively, but instead of running `runAllBatch`,
run `runEXPERIMENTGraphics`, replacing `EXPERIMENT` with the name of the experiment you want to run
(namely, with the name of the YAML simulation file, that can be found under `src/main/yaml` folder).

If in doubt, run `./gradlew tasks` to see the list of available tasks.

To make changes to existing experiments and explore/reuse,
we recommend to use the IntelliJ Idea IDE.
Opening the project in IntelliJ Idea will automatically import the project, download the dependencies,
and allow for a smooth development experience.


