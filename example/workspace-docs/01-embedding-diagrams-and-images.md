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

Static assets can be included in the generated site, but need to be copied over
to the `{docsDir}/img` folder. Once you've done that, you can reference them in
your markdown files like this:

```markdown
![A nice picture](../img/nice-picture.png)
```
