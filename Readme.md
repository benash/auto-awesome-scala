# exp-awesome-scala
This experiment was an attempt to build an awesome Scala list in a semi-automated fashion by querying Maven and GitHub. I flagged Scala projects by looking for a Pom dependency on `scala-lang`; this worked pretty well, but it included some false negatives, like its exclusion of Mill.

I'm abandoning this approach and instead favor querying GitHub directly.

## Usage
### Syntax
```shell
GITHUB_TOKEN=my-token sbt "run <days> <start> <limit>"
```
### Example
Query for Maven artifacts added in the past 30 days, starting with the very first (0th), with a limit of 1000.
```shell
GITHUB_TOKEN=my-token sbt "run 30 0 1000"
```

## Caching
When artifacts are pulled from Maven, they're stored in `pom-experiment.cache.json` and used in subsequent runs.
