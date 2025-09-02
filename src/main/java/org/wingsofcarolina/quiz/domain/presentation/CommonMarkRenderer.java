package org.wingsofcarolina.quiz.domain.presentation;

import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
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

  public static Paragraph renderToParagraph(String input) {
    graph = new Paragraph();
    firstLine = true;

    Node document = parser.parse(input);
    document.accept(visitor);

    return graph;
  }

  static class MyVisitor extends AbstractVisitor {

    private boolean emphasis = false;
    private boolean strong = false;
    private boolean head = false;
    private boolean table = false;
    private com.itextpdf.layout.element.Table imbeddedTable = null;
    private int rows = 0;
    private int columns = 0;
    private int rowcount = -1;
    private int columnCount = -1;

    private boolean list = false;
    private com.itextpdf.layout.element.List imbeddedList = null;
    private boolean noNewline;

    @Override
    public void visit(Text element) {
      String literal = element.getLiteral();
      com.itextpdf.layout.element.Text text = new com.itextpdf.layout.element.Text(
        literal
      );
      if (emphasis || head) text.simulateBold();
      if (strong) text.simulateItalic().simulateBold();
      if (table == true) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text));
        if (imbeddedTable != null) imbeddedTable.addCell(cell);
      } else if (list == true) {
        if (imbeddedList == null) {
          imbeddedList = new com.itextpdf.layout.element.List();
        }
        imbeddedList.add(new com.itextpdf.layout.element.ListItem(literal));
      } else {
        if (list && !firstLine) {
          graph.add("\n");
          if (!noNewline) {
            graph.add("\n");
          }
        }
        firstLine = false;
        graph.add(text);
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
      graph.add("\n\n");
      graph.add(imbeddedList);
      graph.add("\n\n");
      imbeddedList = null;
      list = false;
    }

    public void visit(CustomNode node) {
      if (node instanceof TableHead) {
        //		    	LOG.info("TableHead {}", node);
        if (imbeddedTable == null) {
          graph.add("\n\n");
          columnCount = getColumnCount(node.getFirstChild());
          imbeddedTable = new com.itextpdf.layout.element.Table(columnCount);
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
          graph.add("\n\n");
          columnCount = getColumnCount(node);
          imbeddedTable = new com.itextpdf.layout.element.Table(columnCount);
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
        graph.add(imbeddedTable);
        graph.add("\n\n");
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
