package io.github.mwz.structurizr.mkdocs

extension (s: String) def toOption: Option[String] = Option(s).filter(_.trim().nonEmpty)
