## Glance setup

This guide assumes that you are setting up Glance via a shell session on a 
server running Ubuntu 16.04 (aka xenial). Preferably, this server will be "as new"; i.e. the operating system will have 
been freshly installed and be mostly unmodified. The standard Ubuntu image from most popular webhosts (AWS, Azure, 
etc...) will work perfectly.

If you are comfortable with the technology involved in building Glance and don't want to 
use the "default" configuration for some reason then of course you can just build it yourself. To that end we also
provide a list of software prerequisites and a brief description of the build structure. Most people should try and use 
Ubuntu 16 however, as this has been well tested and we produce a number of helpful scripts for installations with that OS. 

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

8. Remove the .env file and restore the defaults file

9. Build the cli tools

10. Copy the data, according to our Data format, to some location on your server's hard drive

11. Transform the data

12. Generate the Surveys

13. Load the data to support the surveys

14. Check the surveys are working. 

15. Download some results.

16. Take a full backup of the data.

17. Restore a full backup of the data.

8. Back in a shell session, its now time to import some data to our Glance install, and generate some surveys! 

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


