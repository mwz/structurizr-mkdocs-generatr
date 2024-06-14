githubTokenSource := TokenSource.Or(
  TokenSource.Environment("GITHUB_TOKEN"),
  TokenSource.GitConfig("github.token")
)
