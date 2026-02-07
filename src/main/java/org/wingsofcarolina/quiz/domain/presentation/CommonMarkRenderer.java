package org.wingsofcarolina.quiz.domain.presentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Emphasis;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.openpdf.text.Chunk;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonMarkRenderer {

  private static final Logger LOG = LoggerFactory.getLogger(CommonMarkRenderer.class);

  private static List<Extension> extensions = Arrays.asList(TablesExtension.create());
  private static Parser parser = Parser.builder().extensions(extensions).build();
  private static HtmlRenderer htmlRenderer = HtmlRenderer
    .builder()
    .extensions(extensions)
    .build();
  private static MyVisitor visitor = new MyVisitor();

  private static Paragraph graph;
  private static boolean firstLine = true;
  private static boolean noNewline = false;
  private static List<Element> elements;
  private static boolean graphHasContent;
  private static float fontSize = 10f;

  public static String renderAsHtml(String input) {
    Node document = parser.parse(input);
    String output = htmlRenderer.render(document);
    String result = removeAll(output, "<p>");
    result = removeLast(result, "</p>");
    return result;
  }

  public static String removeAll(String input, String cut) {
    String result = input;
    while (result.indexOf(cut) != -1) {
      result = removeFirst(result, cut);
    }
    return result;
  }

  public static String removeFirst(String input, String cut) {
    StringBuilder sb = new StringBuilder(input);
    if (input.length() < cut.length()) {
      return input;
    } else if (input.indexOf(cut) != -1) {
      int start = input.indexOf(cut);
      StringBuilder afterRemove = sb.delete(start, start + cut.length());
      return afterRemove.toString();
    } else {
      return input;
    }
  }

  public static String removeLast(String input, String cut) {
    StringBuilder sb = new StringBuilder(input);
    if (input.length() < cut.length()) {
      return input;
    } else if (input.lastIndexOf(cut) != -1) {
      int start = input.lastIndexOf(cut);
      StringBuilder afterRemove = sb.delete(start, start + cut.length());
      return afterRemove.toString();
    } else {
      return input;
    }
  }

  // Legacy method retained for compatibility; prefers plain text rendering
  public static Paragraph renderToParagraph(String input) {
    Paragraph p = new Paragraph();
    Node document = parser.parse(input);
    elements = new ArrayList<>();
    graph = p;
    graphHasContent = false;
    firstLine = true;
    document.accept(visitor);
    // Add accumulated text to paragraph
    return graph;
  }

  public static List<Element> renderToElements(String input) {
    return renderToElements(input, 10f);
  }

  public static List<Element> renderToElements(String input, float fontSizeParam) {
    elements = new ArrayList<>();
    graph = new Paragraph();
    graphHasContent = false;
    firstLine = true;
    fontSize = fontSizeParam;
    Node document = parser.parse(input);
    document.accept(visitor);
    if (graphHasContent) {
      elements.add(graph);
    }
    return elements;
  }

  static class MyVisitor extends AbstractVisitor {

    private boolean emphasis = false;
    private boolean strong = false;
    private boolean head = false;
    private boolean table = false;
    private PdfPTable imbeddedTable = null;
    private int rows = 0;
    private int columns = 0;
    private int rowcount = -1;
    private int columnCount = -1;

    private boolean list = false;
    private org.openpdf.text.List imbeddedList = null;
    private boolean noNewline;

    @Override
    public void visit(Text element) {
      String literal = element.getLiteral();
      int style = Font.NORMAL;
      if (strong) {
        style = Font.BOLDITALIC;
      } else if (emphasis || head) {
        style = Font.BOLD;
      }
      Font font = FontFactory.getFont("Helvetica", fontSize, style);
      if (table) {
        PdfPCell cell = new PdfPCell();
        Paragraph p = new Paragraph();
        p.add(new Chunk(literal, font));
        cell.addElement(p);
        cell.setBorder(PdfPCell.NO_BORDER);
        if (imbeddedTable != null) imbeddedTable.addCell(cell);
      } else if (list) {
        if (imbeddedList == null) {
          imbeddedList = new org.openpdf.text.List(org.openpdf.text.List.UNORDERED);
        }
        org.openpdf.text.ListItem li = new org.openpdf.text.ListItem(
          new Phrase(literal, font)
        );
        imbeddedList.add(li);
      } else {
        if (list && !firstLine) {
          graph.add(Chunk.NEWLINE);
          if (!noNewline) {
            graph.add(Chunk.NEWLINE);
          }
        }
        firstLine = false;
        graph.add(new Chunk(literal, font));
        graphHasContent = true;
      }
    }

    @Override
    public void visit(Emphasis element) {
      emphasis = true;
      visitChildren(element);
      emphasis = false;
    }

    public void visit(StrongEmphasis element) {
      strong = true;
      visitChildren(element);
      strong = false;
    }

    @Override
    public void visit(ListItem listItem) {
      visitChildren(listItem);
    }

    @Override
    public void visit(OrderedList orderedList) {
      //	    	LOG.info("OrderedList {}", orderedList);
      noNewline = true;
      visitChildren(orderedList);
      noNewline = false;
    }

    @Override
    public void visit(BulletList bulletList) {
      //	    	LOG.info("BulletList {}", bulletList);
      list = true;
      visitChildren(bulletList);
      if (graphHasContent) {
        elements.add(graph);
        graph = new Paragraph();
        graphHasContent = false;
      }
      if (imbeddedList != null) {
        elements.add(imbeddedList);
      }
      Paragraph spacer = new Paragraph();
      spacer.add(Chunk.NEWLINE);
      spacer.add(Chunk.NEWLINE);
      elements.add(spacer);
      imbeddedList = null;
      list = false;
    }

    public void visit(CustomNode node) {
      if (node instanceof TableHead) {
        //		    	LOG.info("TableHead {}", node);
        if (imbeddedTable == null) {
          columnCount = getColumnCount(node.getFirstChild());
          imbeddedTable = new PdfPTable(columnCount);
          imbeddedTable.setWidthPercentage(100);
        }
        head = true;
        table = true;
      } else if (node instanceof TableRow) {
        //		    	LOG.info("TableRow {}", node);
        rows++;
        columns = 0;
        table = true;
      } else if (node instanceof TableBody) {
        if (imbeddedTable == null) {
          columnCount = getColumnCount(node);
          imbeddedTable = new PdfPTable(columnCount);
          imbeddedTable.setWidthPercentage(100);
        }
        rows = 0;
        rowcount = getRowCount(node);
        //		    	LOG.info("TableBody {}", node);
        head = false;
        table = true;
      } else if (node instanceof TableCell) {
        //		    	LOG.info("TableCell {}", node);
        columns++;
        table = true;
      } else {
        LOG.info("CustomNode {}", node);
      }
      visitChildren(node);

      // Make sure we are not in a header, and have accounted for
      // all rows and all columns. This is because there is no
      // event which actually marks the end of a table (damn it).
      if (!head && imbeddedTable != null && rows == rowcount && columns == columnCount) {
        if (graphHasContent) {
          elements.add(graph);
          graph = new Paragraph();
          graphHasContent = false;
        }
        elements.add(imbeddedTable);
        Paragraph spacer = new Paragraph();
        spacer.add(Chunk.NEWLINE);
        spacer.add(Chunk.NEWLINE);
        elements.add(spacer);
        imbeddedTable = null;
        head = false;
        table = false;
      }
    }

    private int getRowCount(Node node) {
      int rows = 1;
      Node next = node.getFirstChild();
      while (next != null && next != node.getLastChild()) {
        next = next.getNext();
        rows++;
      }
      return rows;
    }

    private int getColumnCount(Node node) {
      int columns = 0;
      Node next = node.getFirstChild();
      while (next != null) {
        next = next.getNext();
        columns++;
      }
      return columns;
    }
  }
}
