## Embedding diagrams and images

This page showcases the ability to embed diagrams and static images in
documentation.

### Embedding diagrams

Diagrams can be embedded using the `embed:` syntax:

```markdown
![System Landscape Diagram](embed:SystemLandscape)
```

See also: <https://www.structurizr.com/help/documentation/diagrams>

#### Example: Embedded diagram

![System Landscape Diagram](embed:SystemLandscape)

### Embedding static images

Static assets can be included in the generated site, using the `--assets-dir`
command-line flag.

When, for example, you would like to embed a nice picture which is located in
the `pictures` directory, you can do that as follows:

```sh
java -jar structurizr-mkdocs-generatr.jar --assets-dir pictures
```

Then, you can reference the picture in your markdown files like this:

```markdown
![A nice picture](../img/nice-picture.png)
```

#### Example: Embedded picture

[Sun](https://www.flickr.com/photos/schmollmolch/4937297813/), by Christian
Scheja

![A nice picture](../img/nice-picture.png)
