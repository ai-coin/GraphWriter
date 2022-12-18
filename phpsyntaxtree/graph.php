<?php

// grapn.php - Generate a syntax tree
// Copyright (c) 2003-2005 Andre Eisenbach <andre@ironcreek.net>
// Modified to run from the command line -- Jack Harris.
//
// graph.php is part of phpSyntaxTree.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

require_once( "src/CElementList.php" );
require_once( "src/CStringParser.php" );
require_once( "src/CTreeGraph.php" );

$outputfile = $argv[1]; // filename
$data = $argv[2]; // parse data
printf("\noutputFile1...\n");
printf($outputfile);
printf("\ndata...\n");
printf($data);

$color = 1;
$triangles = FALSE;
$antialias = 1;   // smooth lines
$autosub = 0;
$font = 'Vera.ttf';
$fontsize = 14;  // readable font, and readable subscript font
// Validate the phrase and draw the tree
$stringParser = new CStringParser( $data );

if ($stringParser->Validate()) {
  // If all is well, go ahead and draw the graph ...

  $stringParser->Parse();

  if ($autosub) {
    $stringParser->AutoSubscript();
  }

  $elementList = $stringParser->GetElementList();

  // Draw the graph into a file

  $fontpath = dirname($_SERVER['SCRIPT_FILENAME']) . '/ttf/';

  $graph = new CTreegraph(
          $elementList,
          $color,
          $antialias,
          $triangles,
          $fontpath .
          $font,
          $fontsize);
  $graph->Save($outputfile);
} else {
  printf("Error: Phrase could not be parsed correctly.\n");
}
?>
