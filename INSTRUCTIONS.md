## Glance setup

This guide assumes that you are setting up Glance via a shell session on a 
server running Ubuntu 16.04 (aka xenial). Preferably, this server will be "as new"; i.e. the operating system will have 
been freshly installed and be mostly unmodified. The standard Ubuntu image from most popular webhosts (AWS, Azure, 
etc...) will work perfectly.

If you are comfortable with the technology involved in building Glance and don't want to 
use the "default" configuration for some reason then of course you can just build it yourself. To that end we also
provide a list of software prerequisites and a brief description of the build structure. Most people should try and use 
Ubuntu 16 however, as this has been well tested and we provide a number of helpful scripts for installations with that OS. 

The guide does assume very basic familiarity with the use of a bash shell on Ubuntu, though all commands to be executed 
are listed verbatim and explained. 

### Installing Glance on Ubuntu 16 (the first path) 

1. Check that the packages already installed on your server are up to date: 
    ```bash
    sudo apt-get update
    sudo apt-get upgrade  
    ```
    
2. Check that the command line tool `git` is installed by executing `git --version`. You should expect to see output 
like `git version 2.7.4`. If you do not, then you should run `sudo apt-get install git` and agree to install to any prompts.

3. Clone this repository to some location on your machine and move into it: 
    ```bash
    git clone https://github.com/NewcastleComputingScience/student-outcome-accelerator.git
    cd student-outcome-accelerator
    ```
       
4. Make sure that the install scripts downloaded along with the Glance source code are executable:
    ```bash
    chmod -R +x bin/
    ```       
       
5. The default passwords used by various internal pieces of the Glance application are listed in the file `.env` in the 
source root. You should make a copy of the defaults, then edit the `.env` file to provide real values.
    ```bash
    cp .env defaults.env
    nano .env
    ```
   
6. Run the install script:
    ```bash
    sudo ./bin/glance-setup.sh
    ```    
   **Note**: This script will take **many** minutes and produce a lot of output. It has a lot of things to download, 
   build or install so this is understandable. If you are performing this installation on a remote server via `ssh` 
   (likely) then it is recommended that you work inside a [tmux](https://hackernoon.com/a-gentle-introduction-to-tmux-8d784c404340)
   or [screen](https://nathan.chantrell.net/linux/an-introduction-to-screen/) session. This will prevent the installation 
   process from halting halfway through due to a dropped `ssh` connection.
   
   Also note that throughout the installation process you may occasionally be prompted to approve the installation of 
   things (i.e. see something like: `Install foo? It will take 8Mb. [Y/n]:`). You should agree to all such prompts. 
   
   Once the installation has finished, you should get the cursor back (be able to type commands into the shell again) and 
   see output like the following immediately above: 
   ```bash
   Creating studentoutcomeaccelerator_glance-eval-backend_1 ...
   Creating studentoutcomeaccelerator_glance-eval-backend_1 ... done
   Creating studentoutcomeaccelerator_glance-eval-frontend_1 ...
   Creating studentoutcomeaccelerator_glance-eval-frontend_1 ... done
   ```
   
7. Check that the installation is working by vistiting the domain or ip address for your server in your web server. You 
should see a webpage with almost no content (yet) but 3 large titles, _Rank students_, _Detailed view_ and _Submit survey_.

8. Remove the `.env` file containing your passwords and replace it with the backed up defaults:
    ```bash
    rm .env && mv defaults.env .env
    ``` 
    **Note**: You must remember your passwords after this point.

### Importing data and generating Glance surveys

Now that you have a working and web accessible installation of the Glance software - its time to actually generate some 
surveys. The steps below walk you through the simple process of building a command line tool from the sources in this 
folder.

This command line tool is used for:

* Transforming and importing data into Glance.
* Generating Glance surveys.
* Exporting survey results from Glance.

The commands for each of these tasks will also be listed and explained.

**Note**: Continuing on from the previous guide, it is assumed that all the commands listed below are executed within 
the root of this folder (in the same location as this file). To check that this is correct, you can run the command 
`pwd`. You should see something along the lines of `/home/ubuntu/student-outcome-accelerator`. 

1. Build the glance cli tools:
    ```bash
    sbt glance-eval-cli/assembly
    mv glance-eval-cli/target/scala-2.11/soar-glance-eval-cli.jar bin/
    ```
    **Note**: As with earlier steps in this guide, the first command above take some time (usually less than two minutes)
    and produce a lot of output. This is to be expected.

2. Copy the data, formatted according to our specified [schema](SCHEMA.md), to some location on your server's hard drive. If your
server is remote (accessed using `ssh`) you can send files from your **local machine** as follows:

    * Assuming you are using Linux or MacOS on your **local machine**, then execute the following command in your 
    **local terminal**:  

    ```bash
    scp -rp /local/location/data username@remote.address:/remote/location/
    ```

    The `username` and `remote.address` should be the same as those you use to log into your **remote machine** via 
    `ssh`. Unless you use a key, this command will also prompt you for a password, which is also the same as the one you
    use for `ssh`. If you **do** use a key to access your remote server, then that is currently out of scope for this 
    documentation. 
    
    * Assuming you are using Windows on your local machine, then ensure you have installed the 
    [PuTTy](https://www.chiark.greenend.org.uk/~sgtatham/putty/latest.html) program (you probably have, as PuTTy is by 
    far the most common way of working with (linux) **remote machines** on Windows). PuTTy comes with a utility called 
    `pscp`, the documentation for which is [here](https://tartarus.org/~simon/putty-snapshots/htmldoc/Chapter5.html#pscp).
    Once you have setup `pscp` as per the documentation, you can run the following command in `cmd.exe`:
    
    ```bash
    pscp -rp C:\local\location\data username@remote.address:/remote/location
    ```
    
    **Note**: The local Windows filesystem locations may be given using either forward (/) or backward (\) slashes, but 
    the remote linux filesystem location **must** be given using forward (/) slashes. Also note that if you don't know 
    how to start `cmd.exe` on Windows you can do it by clicking **Start**, then **Run**, typing **cmd** and hitting enter.  


3. On your server again (not your local machine), we need to prune and transform the data to be presented in Glance 
surveys. This can be done with the following command:
    ```bash
    ./bin/glance-cli.sh transform -m /location/data/Marks.csv -o /location/to/output/transformed/csvs -p CSC -y 2015 -s 2 \
        --cluster /location/data/ClusterSession.csv --recap /location/data/RecapSession.csv  
    ```
    
    **Note**: the above is only an example of a `transform` command. There are many possible command line options with 
    distinct meanings. These are detailed in full below, or if you type the command: `./bin/glance-cli.sh transform --help`.
    
    ```
    Glance Data Transformer 0.1.x
    Usage: GlanceTransform [options]

        -m, --marks <file>     marks is a required .csv file containing student marks.
        -o, --output <path>    output is a required parameter specifying the directory to write transformed data to.
        -p, --prefix e.g. CSC  prefix is a required parameter which indicates the module code prefix for which we should transform marks.
        -y, --year e.g. 2015   year is a required parameter which indicates the earliest academic year for which to transform marks.
        -s, --stage e.g. 2     stage is a required parameter which indicates the earliest academic stage for which to transform marks.
        --cluster <file>       cluster is an optional .csv file containing student sessions using University clusters.
        --recap <file>         recap is an optional .csv file containing student sessions using the ReCap video lecture service.
        --printed <file>       printed is an optional .csv file containing student print events.
        --vle <file>           vlePath is an optional .csv file containing student VLE sessions.
        --meetings <file>      meetingsPath is an optional .csv file containing student meeting records.
    ```
    
    The first 5 options are compulsory, whilst the remaining 5 are optional depending on which data files you need for 
    the [visualisations](VISUALISATIONS.md) you intend to include in your survey. This command, when run, will create 
    the output directory specified by the `-o` option and subsequently create several small `.csv` files in said 
    directory. These small files will contain only those records which may be relevant to the survey generated in the 
    next step.
    
    **Note**: You may assume everything other than step 2 takes place on a server (remote or otherwise). 

4. Now that we have pruned and filtered the data, it is time to generate the surveys themselves and persist them in 
Glance's database: This can be done with the following command:
    ```bash
    ./bin/glance-cli.sh generate -i /location/of/transformed/data --modules CSC3621,CSC3222 \
       --visualisations recap_vs_time,cluster_vs_time,stud_avg_vs_time,stud_module_scores
    ```
    
    **Note**: The above is only an example of a `generate` command. There are many possible command line options with
    distinct meanings. These are detailed in full below, or if you type the command: `./bin/glance-cli.sh generate --help`
    ```
     
    Glance Survey Generator 0.1.x
    Usage: GlanceGen [options]

        -i, --input <directory>       
                           a required path containing the output of a transform step.
        -m, --modules e.g. CSC1021, CSC2024...
                           a required list of modules for which to generate surveys. Unless the --collection option is used, only one survey will be generated per module.
        -v, --visualisations e.g. recap_vs_time,stud_module_scores,...
                           visualisations is a required parameter detailing the list of visualisations to use in a Glance survey.
        -n, --num-students e.g. 10
                           an optional parameter specifying how many student records to include in each generated survey.
        -s, --students e.g. 3789,11722,98,...
                           an optional parameter specifying exactly which student records to include in the generated surveys. Note that if --students are provided then --num-students and --collection will be ignored.
        -c, --collection <int>   
                           an optional number of surveys to generate in series. They will all use different students and may be completed one after another.
        -r, --random-seed <int>  
                           an optional integer to use as a seed when randomly (uniformly) selecting student records to include in a survey.
    ```
    
    The first 3 options are compulsory, whilst the remaining 4 are optional depending on the surveys you wish to generate.
    When run, the above command will create surveys in the Glance database and return links where they may be 
    accessed and conducted by instructors. 
    
    **Note**: Such a large number of options are included in the `glance-cli` tools in order to support configurability,
    however they also create edge cases where combinations of commands or options may fail to behave as they should. 
    For example, if you fail to provide files in a `transform` step which are then required by visualisations you select
    in a `generate` step, Glance will not generate surveys. Additionally, if you use `--num-students` to specify more 
    students than have marks for the chosen `--modules` then Glance will not generate surveys. 
    
    Effort has been made to make the error reporting of the `glance-cli` tools fairly comprehensive. If you encounter such 
    an error, please adjust your combination of command line options accordingly. If you encounter no error, but Glance
    still isn't generating surveys as you believe it should, please submit an [issue](). 
    
    **Note**: The optional `random-seed` option is used if you wish to ensure that `glance-cli`  generates the exact same
    surveys as on a previous execution (selects the same students etc...). Most of the time it can be safely ignored.
    
5. Check the surveys are working. 

6. Download some results.

### Performing miscellaneous tasks 

1. Take a full backup of the data.

2. Restore a full backup of the data.

3. Re-installing Glance if there are source changes

4. Removing Glance completely. 

5. Starting and Stopping Glance

6. Rebranding Glance

### Support and issues

If you have any issues with any of the steps in this Guide, please submit an issue here according to the following template:

### Other information

If you have followed the above guides successfully you may safely ignore this section.

##### Software prerequisites for Glance


##### Glance build description


1. Create Postgres database with correct details using the following two commands:
    ```
    psql -c 'create user postgres createdb'
    psql -c 'create database glance_eval' -U postgres
    ```

2. Download and extract datafiles from provided NCL Dropoff link.

3. Start the `sbt` console in the `soar` directory by executing the `sbt` command.

4. Once the sbt console has started, generate the database schema using the following command: 
    ```
    glance-evalJVM/flywayMigrate
    ``` 
    
5. Unfortunately the prepackaged versions of the cli tools are failing silently at the moment. I'm figuring out why as 
 we speak, but in the mean time run the `transform` job (which prepares sql12 data for insertion into the glance 
 database) using the following command in sbt: 
    ```
    glance-eval-cli/run transform -c /Location/Of/CSClusterSessions.csv -r /Location/Of/RecapSessions.csv -m /Location/Of/NessMarks.csv -o /Directory/To/Write/Transformed/Csvs -p CSC -y 2015 -s 2
    ``` 
    
6. Run the `generate` job (which creates the surveys in the glance database) using the following command in sbt:
    ```
    glance-eval-cli/run generate -i /Location/Of/marks.csv --modules CSC3621,CSC3222,CSC2026
    ```
    **Note** that `marks.csv` is generated by the previous job.
    
7. Run `load-support` job (which loads cluster and recap info transformed by step 5, into the glance database) using the
following command in sbt: 
    ```
    glance-eval-cli/run load-support -c /Location/Of/clusterSessions.csv -r /Location/Of/recapSessions.csv
    ```
    **Note** that `clusterSessions.csv` and `recapSessions.csv` are generated by the `transform` job.

8. Exit the `sbt` console and manually execute .sql dump in `glance-eval-cli/cs-surveys-bin` against the glance 
postgres database. This loads module titles, descriptions, keywords, and start dates/durations (where needed).

9. Restart the sbt console (using `sbt`).

10. Start the survey app with the following command:
    ```
    glance-evalJVM/reStart
    ``` 
    Once you have done this, the api is available [here](http://localhost:8080), whilst the front-end is available  
    [here](http://localhost:12345/glance-eval/js/target/scala-2.11/classes/index-dev.html).
    
11. If you want to load specific surveys (rather than the default) then you need to use the following url structure: 
`.../index-dev.html#survey/{id}` where `{id}` corresponds to the uuid string for the the survey in question. E.g:

    ```
    .../index-dev.html#survey/13927f7f-ded8-4862-a61f-66b7dd90b709   
    ```
    
    I suggest we keep a list of the links and the surveys they correspond to so that we can quickly load them at the 
    start of each in person session.

### Updating (no data changes)

1. Pull down the soar repo
2. re-run `glance-evalJVM/flyWayMigrate` 
3. Perform steps 8-9 above.

### Updating (data changes)
1. Pull down the soar repo
2. Backup database with the `pg_dump` utility or similar
3. Run `glance-evalJVM/flywayClean` then `glance-evalJVM/flywayMigrate`. **Note** that the clean command will wipe the 
database.
4. Rerun steps 4-9 above. 


