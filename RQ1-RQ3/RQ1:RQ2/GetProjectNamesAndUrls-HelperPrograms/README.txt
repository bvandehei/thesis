In this folder, there are two helper programs that are used to generate a list of
Apache Jira projects and their corresponding git urls for cloning.

The first program is called GetProjectNames.java. This project uses the apache
jira API to get a list of all projects keys and corresponding project names for
apache jira projects with at least 100 closed bugs. The out put of this program is
called Projects.csv which has a project key and name per line.

The ProjectEditted.csv is the same as Projects.csv except with the names of a few
projets editted manually so that the input is better for the next program.

The second project is called GetProjectURLs.java. This project reads in the
ProjectsEditted.csv and uses the gihub api and searches for repos containing the
name of project where the user is apache. The program creates a csv called
ProjectsAndUrls.csv where each line contains the key, name, and a list of git urls
for cloning of each repo associated with the name. If no repo was found, no urls
listed.

ProjectsAndUrlsEditted.csv is the same as ProjectsAndUrls.csv except a few manually
added urls for some repos where the program was not able to find them with the git
search. There are a few projects where no repos were found, these will be removed
from the list in the next program.
