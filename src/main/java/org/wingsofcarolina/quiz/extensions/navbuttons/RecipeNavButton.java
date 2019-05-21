package org.wingsofcarolina.quiz.extensions.navbuttons;

public class RecipeNavButton extends NavButton {

	@Override
	public String html() {
		StringBuffer sb = new StringBuffer();
		sb.append("<script>function viewRecipe() {"
				+ "var recipeId = prompt(\"Recipe ID\");"
				+ "if (recipeId != null) { window.location.href = \"/recipe?quiz=\" + recipeId + \"\";\n" + 
				" } }</script>");
		sb.append("<a ");
		if (active) sb.append("class=\"active\"");
		sb.append(" onclick=viewRecipe()>Recipe</a>\n");
		return sb.toString();
	}
}
