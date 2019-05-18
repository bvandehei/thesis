This Quality model calculates the number of bugs with a problem and percentage for
project with at least 100 closed bugs. The input is ProjectsAndUrlsEditted.csv in
the GetProjectNamesAndUrls-HelperPrograms folder. See the README in that folder
for information regarding how that file is generated. This program reads projects
keys and corresponding git urls from the csv input file. It clones and search repos
for each project to get the date of the last commit for each bug. It also uses
apache api to get the ticket creation date and dates of affected versions for each
bug in each apache project. Below lists details of each problem found in the recorded
affect versions. This program reports the following for each apache jira project
with more than 100 closed bugs in the QualityModel.csv.

Columns in the CSV:
Key - Project key

Number of Closed and Linked Bugs - the number of bugs that are closed and have
one Git commit with that ID in the comment (i.e., you can retrieve from Git the
fix version). This excludes bugs where AV == TCV == FV. Everything below applies to these bugs only.

Number of C/L Bugs with no AV: the number of closed bugs in the project where the
affect version field is empty or all reported versions are without dates

Percent of C/L Bugs with no AV: Number of C/L Bugs with no AV / Number of Closed
and Linked Bugs

Number of C/L Bugs with First AV after Ticket Creation: The number
of bugs where the first AV is after the ticket creation

Percent of C/L Bugs with First AV after Ticket Creation: Number of
C/L Bugs with First AV after Ticket Creation 


Number of C/L Bugs with a Problem: The total number of bugs with one of the above
problems. These problems by definition do not overlap.

Percent of C/L Bugs with a Problem: Number of C/L Bugs with a Problem / Number
of Closed and Linked Bugs
