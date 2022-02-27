package org.wingsofcarolina.quiz.domain.presentation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.QuizBuildException;
import org.wingsofcarolina.quiz.domain.Answer;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.resources.Quiz;
import org.wingsofcarolina.quiz.resources.QuizContext;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;


public class PDFGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(PDFGenerator.class);

	private PdfFont normal;
	private QuizContext context;
	
	public PDFGenerator(QuizContext context) throws IOException {
		this.context = context;
		normal = PdfFontFactory.createFont(StandardFonts.HELVETICA, true);
	}

	public ByteArrayInputStream generate(Quiz quiz) throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {		
		if (quiz != null) {
			return generateQuiz(quiz);
		} else {
			throw new QuizBuildException("Null pointer for quiz entity");
		}
	}

	public ByteArrayInputStream generate(Question question) throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {		
		if (question != null) {
			return generateQuestion(question);
		} else {
			throw new QuizBuildException("Null pointer for question entity");
		}
	}


	public ByteArrayInputStream generate(List<Question> questions) throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
		if (questions != null) {
			return generateQuestion(questions);
		} else {
			throw new QuizBuildException("Null pointer for question entity");
		}
	}
	
	public ByteArrayInputStream generateQuestion(Question question) throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
		// Create the document
		ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(inMemoryStream);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setBottomMargin(110.0f);
		document.setFont(normal).setFontSize(10);
		
		SolidLine line = new SolidLine(1f);
		LineSeparator ls = new LineSeparator(line);
		ls.setMargin(5);
		document.add(ls);

		document.add(addQuestion(0, question));

		document.add(ls);
		
		// Finalize the document
		document.close();

		return new ByteArrayInputStream(inMemoryStream.toByteArray());

	}

	public ByteArrayInputStream generateQuestion(List<Question> questions) throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
		// Create the document
		ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(inMemoryStream);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setBottomMargin(110.0f);
		document.setFont(normal).setFontSize(10);
		
		SolidLine line = new SolidLine(1f);
		LineSeparator ls = new LineSeparator(line);
		ls.setMargin(5);
		document.add(ls);

		int i = 1;
		for (Question question : questions) {
			document.add(addQuestion(i++, question));
			document.add(ls);
		}
		
		// Finalize the document
		document.close();

		return new ByteArrayInputStream(inMemoryStream.toByteArray());

	}

	public ByteArrayInputStream generateQuiz(Quiz quiz) throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {		
		// Create the document
		ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(inMemoryStream);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setBottomMargin(110.0f);
		document.setFont(normal).setFontSize(10);
		PageXofY event = new PageXofY(pdf, quiz);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event);
        
		// First lets flesh out the document header
		addDocumentHeader(quiz, document);

		// Generate all the questions
		Integer number = 1;
		for (Question question : quiz.getQuestions()) {
			document.add(addQuestion(number, question));
			document.add(new Paragraph("\n"));
			number++;
		}

		// Finalize the document
        event.writeTotal(pdf);
		document.close();

		return new ByteArrayInputStream(inMemoryStream.toByteArray());
	}
	
	private IBlockElement addQuestion(Integer index, Question question) {
		char[] characters = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L' };
		Float CELL_WIDTH = 30.0f;

		Table table = new Table(new float[] {1, 2});
		table.setWidth(UnitValue.createPercentValue(100));
		table.setKeepTogether(true);
		table.setFont(normal);

		Cell cell = new Cell();
		cell.setBorder(Border.NO_BORDER);
		cell.setTextAlignment(TextAlignment.RIGHT);
		cell.add(new Paragraph(index.toString() + " : "));
		cell.setWidth(CELL_WIDTH);
		table.addCell(cell);
		
		cell = new Cell();
		cell.setBorder(Border.NO_BORDER);
		cell.setFontSize(12);
		Paragraph graph = question.getQuestionAsIText();
		// TODO: Remove the question number from the PDF once we go live
		String questionId = new Long(question.getQuestionId()).toString();
		graph.add(new Text(" (" + questionId + ")"));
		cell.add(graph);
		table.addCell(cell);

		// Add an image attachment, if one is requested
		if (question.getAttachment() != null && ! question.getAttachment().equals("NONE")) {
			String imageDir = context.getConfiguration().getImageDirectory() + "/";
		    try {
				Image image = new Image(ImageDataFactory.create(imageDir + question.getAttachment()));
				float imageWidth = image.getImageWidth();
				if (imageWidth > 400f) {
					float scale = imageWidth / 450.0f;
					float imageHeight = image.getImageHeight() / scale;
		            image.scaleAbsolute(450f, imageHeight);
				}
				cell = new Cell();
				cell.setBorder(Border.NO_BORDER);
				cell.add(new Paragraph("\n"));
				table.addCell(cell);
				cell = new Cell();
				cell.setBorder(Border.NO_BORDER);
				cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
				cell.setHorizontalAlignment(HorizontalAlignment.CENTER);
				cell.add(image);
				table.addCell(cell);
			} catch (com.itextpdf.io.IOException|MalformedURLException e) {
				LOG.error("Could not find requested image attachment : {}", question.getAttachment());
			}
		}

		if (question.isMultipleChoice()) {
			int aIndex = 0;
			if (question.getAnswers() == null || question.getAnswers().size() == 0) {
				cell = new Cell();
				cell.setBorder(Border.NO_BORDER);
				cell.setTextAlignment(TextAlignment.RIGHT);
				cell.add(new Paragraph("\n"));
				cell.setWidth(CELL_WIDTH);
				cell.setPaddingTop(0);
				table.addCell(cell);
				
				cell = new Cell();
				cell.setBorder(Border.NO_BORDER);
				cell.setFontSize(12);
				graph.add(new Text("This question has no answers at this time."));
				cell.add(graph);
				table.addCell(cell);
			} else {
				for (Answer answer : question.getAnswers()) {
					cell = new Cell();
					cell.setBorder(Border.NO_BORDER);
					cell.setTextAlignment(TextAlignment.RIGHT);
					cell.add(new Paragraph(characters[aIndex] + " : "));
					cell.setWidth(CELL_WIDTH);
					cell.setPaddingTop(0);
					table.addCell(cell);
					
					cell = new Cell();
					cell.setBorder(Border.NO_BORDER);
					cell.add(answer.getAnswerAsIText());
					cell.setPaddingTop(0);
					table.addCell(cell);
					aIndex++;
				}
			}
		}
		return table;
	}

	private void addDocumentHeader(Quiz quiz, Document document) throws URISyntaxException, MalformedURLException, IOException {
		// Generate header
		byte[] bytes = Files.readAllBytes(new File(context.getConfiguration().getAssetDirectory() + "/WCFC-logo-transparent.jpg").toPath());
		Image img = new Image(ImageDataFactory.create(bytes));
		img.setWidth(150.0f);
		
		Table table = new Table(new float[]{1, 2});
		table.setWidth(UnitValue.createPercentValue(100));

		// Add WCFC Logo
		Cell cell = new Cell();
		cell.add(img);
		cell.setBorder(Border.NO_BORDER);
		table.addCell(cell);

		// Add quiz details
		Cell cell1 = new Cell();
		cell1.setBorder(Border.NO_BORDER);
		Paragraph p = new Paragraph(quiz.getQuizName()).setFontSize(24.0f);
		p.setTextAlignment(TextAlignment.RIGHT);
		cell1.add(p);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String sunsetDate = sdf.format(quiz.getSunsetDate());
		Paragraph p1 = new Paragraph("Review before : " + sunsetDate);
		p1.setTextAlignment(TextAlignment.RIGHT);
		p1.setFontSize(12);
		cell1.add(p1);
		Paragraph p2 = new Paragraph("Quiz ID : ");
		Text id = new Text(new Long(quiz.getQuizId()).toString()).setBold();
		p2.add(id);
		p2.setTextAlignment(TextAlignment.RIGHT);
		p2.setFontSize(12);
		cell1.add(p2);
		cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
		table.addCell(cell1);
		document.add(table);

		// Add quiz details (i.e. who/what/when/how)
		Table details = new Table(new float[] { 1, 1} );
		details.setWidth(UnitValue.createPercentValue(100));
		details.addCell(new Cell().add(new Paragraph("Instructor : ")));
		details.addCell(new Cell().add(new Paragraph("Date  : ")));
		details.addCell(new Cell().add(new Paragraph("Pilot      : ")));
		details.addCell(new Cell().add(new Paragraph("Score : ")));
		document.add(details);

		// Add instructions
		Paragraph instructor = new Paragraph();
		Text text = new Text("Instructor : ");
		instructor.add(text.setBold());
		instructor.add("Please note the final score (subtract " + quiz.pointsPerQuestion()
				+ " points from 100 for each wrong answer) on the checkout form and file the quiz in "
				+ " the Pilot Records folder.");
		document.add(instructor);
//		Paragraph pilot = new Paragraph();
//		text = new Text("Pilot : ");
//		pilot.add(text.setBold());
//		pilot.add("Information required to correctly answer the following questions may be "
//				+ " found in FAR Parts 61 and 91, the WCFC SOPs, and club checklists, documents and "
//				+ " instructional practices.");
//		document.add(pilot);
		
		// Add any Front Matter that might have been configured
		if (context.getVariable("instructions") != null) {
			Paragraph instructions = new Paragraph();
			text = new Text("Pilot : ");
			instructions.add(text.setBold());
			instructions.add(context.getVariable("instructions"));
			document.add(instructions);
		}

		SolidLine line = new SolidLine(1f);
		LineSeparator ls = new LineSeparator(line);
		ls.setMargin(5);
		document.add(ls);
	}

	public Cell createTextCell(String text) throws IOException {
	    Cell cell = new Cell();
	    cell.setBorder(Border.NO_BORDER);
	    Paragraph p = new Paragraph(text);
	    p.setTextAlignment(TextAlignment.RIGHT);
	    cell.add(p);
	    cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
	    return cell;
	}
	
    protected class PageXofY implements IEventHandler {
        
        protected PdfFormXObject placeholder;
        protected float side = 20;
        protected float x = 300;
        protected float y = 85;
        protected float space = 4.5f;
        protected float descent = 3;
        protected Quiz quiz;
        
        public PageXofY(PdfDocument pdf, Quiz quiz) {
        	this.quiz = quiz;
            placeholder = new PdfFormXObject(new Rectangle(0, 0, side, side));
        }
        
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdf.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(
                page.getLastContentStream(), page.getResources(), pdf);
            Canvas canvas = new Canvas(pdfCanvas, pdf, pageSize);
            canvas.setFont(normal);
            canvas.setFontSize(10);
            Paragraph p = new Paragraph()
                .add("Page ").add(String.valueOf(pageNumber)).add(" of");
            canvas.showTextAligned(p, x, y, TextAlignment.RIGHT);
            canvas.showTextAligned(new Paragraph("WCFC " + quiz.getQuizName() + " Quiz").setFontSize(8), 50, y, TextAlignment.LEFT);
            canvas.showTextAligned(new Paragraph("Quiz ID : " + quiz.getQuizId()).setFontSize(8), 550, y, TextAlignment.RIGHT);
            pdfCanvas.addXObject(placeholder, x + space, y - descent);
            pdfCanvas.release();
            canvas.close();
        }
        
        public void writeTotal(PdfDocument pdf) {
            Canvas canvas = new Canvas(placeholder, pdf);
            canvas.setFont(normal);
            canvas.setFontSize(10);
            canvas.showTextAligned(String.valueOf(pdf.getNumberOfPages()),
                0, descent, TextAlignment.LEFT);
            canvas.close();
        }
    }
}
