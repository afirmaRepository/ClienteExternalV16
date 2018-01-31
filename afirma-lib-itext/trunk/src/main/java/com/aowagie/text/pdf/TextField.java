/*
 * Copyright 2003-2005 by Paulo Soares.
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * LGPL license (the "GNU LIBRARY GENERAL PUBLIC LICENSE"), in which case the
 * provisions of LGPL are applicable instead of those above.  If you wish to
 * allow use of your version of this file only under the terms of the LGPL
 * License and not to allow others to use your version of this file under
 * the MPL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the LGPL.
 * If you do not delete the provisions above, a recipient may use your version
 * of this file under either the MPL or the GNU LIBRARY GENERAL PUBLIC LICENSE.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the MPL as stated above or under the terms of the GNU
 * Library General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library general Public License for more
 * details.
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */

package com.aowagie.text.pdf;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import com.aowagie.text.Chunk;
import com.aowagie.text.DocumentException;
import com.aowagie.text.Element;
import com.aowagie.text.Font;
import com.aowagie.text.Phrase;
import com.aowagie.text.Rectangle;

/**
 * Supports text, combo and list fields generating the correct appearances.
 * All the option in the Acrobat GUI are supported in an easy to use API.
 * @author Paulo Soares (psoares@consiste.pt)
 */
public class TextField extends BaseField {

    /** Holds value of property defaultText. */
    private String defaultText;

    /** Holds value of property choices. */
    private String[] choices;

    /** Holds value of property choiceExports. */
    private String[] choiceExports;

    /** Holds value of property choiceSelection. */
    private int choiceSelection;

    private int topFirst;

    private float extraMarginLeft;
    private float extraMarginTop;

    /**
     * Creates a new <CODE>TextField</CODE>.
     * @param writer the document <CODE>PdfWriter</CODE>
     * @param box the field location and dimensions
     * @param fieldName the field name. If <CODE>null</CODE> only the widget keys
     * will be included in the field allowing it to be used as a kid field.
     */
    public TextField(final PdfWriter writer, final Rectangle box, final String fieldName) {
        super(writer, box, fieldName);
    }

    private static boolean checkRTL(final String text) {
        if (text == null || text.length() == 0) {
			return false;
		}
        final char[] cc = text.toCharArray();
        for (final char c : cc) {
            if (c >= 0x590 && c < 0x0780) {
				return true;
			}
        }
        return false;
    }

    private static void changeFontSize(final Phrase p, final float size) {
        for (int k = 0; k < p.size(); ++k) {
			((Chunk)p.get(k)).getFont().setSize(size);
		}
    }

    private Phrase composePhrase(final String text, final BaseFont ufont, final Color color, final float fontSize) {
        Phrase phrase = null;
        if (this.extensionFont == null && (this.substitutionFonts == null || this.substitutionFonts.isEmpty())) {
			phrase = new Phrase(new Chunk(text, new Font(ufont, fontSize, 0, color)));
		} else {
            final FontSelector fs = new FontSelector();
            fs.addFont(new Font(ufont, fontSize, 0, color));
            if (this.extensionFont != null) {
				fs.addFont(new Font(this.extensionFont, fontSize, 0, color));
			}
            if (this.substitutionFonts != null) {
                for (int k = 0; k < this.substitutionFonts.size(); ++k) {
					fs.addFont(new Font((BaseFont)this.substitutionFonts.get(k), fontSize, 0, color));
				}
            }
            phrase = fs.process(text);
        }
        return phrase;
    }

    /**
     * Removes CRLF from a <code>String</code>.
     *
     * @param text
     * @return String
     * @since	2.1.5
     */
    public static String removeCRLF(final String text) {
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            final char[] p = text.toCharArray();
            final StringBuffer sb = new StringBuffer(p.length);
            for (int k = 0; k < p.length; ++k) {
                final char c = p[k];
                if (c == '\n') {
					sb.append(' ');
				} else if (c == '\r') {
                    sb.append(' ');
                    if (k < p.length - 1 && p[k + 1] == '\n') {
						++k;
					}
                } else {
					sb.append(c);
				}
            }
            return sb.toString();
        }
        return text;
    }

    /**
     * Obfuscates a password <code>String</code>.
     * Every character is replaced by an asterisk (*).
     *
     * @param text
     * @return String
     * @since	2.1.5
     */
    public static String obfuscatePassword(final String text) {
    	final char[] pchar = new char[text.length()];
    	for (int i = 0; i < text.length(); i++) {
			pchar[i] = '*';
		}
    	return new String(pchar);
    }

    /**
     * Get the <code>PdfAppearance</code> of a text or combo field
     * @throws IOException on error
     * @throws DocumentException on error
     * @return A <code>PdfAppearance</code>
     */
    public PdfAppearance getAppearance() throws IOException, DocumentException {
        final PdfAppearance app = getBorderAppearance();
        app.beginVariableText();
        if (this.text == null || this.text.length() == 0) {
            app.endVariableText();
            return app;
        }

        final boolean borderExtra = this.borderStyle == PdfBorderDictionary.STYLE_BEVELED || this.borderStyle == PdfBorderDictionary.STYLE_INSET;
        float h = this.box.getHeight() - this.borderWidth * 2 - this.extraMarginTop;
        float bw2 = this.borderWidth;
        if (borderExtra) {
            h -= this.borderWidth * 2;
            bw2 *= 2;
        }
        final float offsetX = Math.max(bw2, 1);
        final float offX = Math.min(bw2, offsetX);
        app.saveState();
        app.rectangle(offX, offX, this.box.getWidth() - 2 * offX, this.box.getHeight() - 2 * offX);
        app.clip();
        app.newPath();
        String ptext;
        if ((this.options & PASSWORD) != 0) {
			ptext = obfuscatePassword(this.text);
		} else if ((this.options & MULTILINE) == 0) {
			ptext = removeCRLF(this.text);
		}
		else {
			ptext = this.text; //fixed by Kazuya Ujihara (ujihara.jp)
		}
        final BaseFont ufont = getRealFont();
        final Color fcolor = this.textColor == null ? GrayColor.GRAYBLACK : this.textColor;
        final int rtl = checkRTL(ptext) ? PdfWriter.RUN_DIRECTION_LTR : PdfWriter.RUN_DIRECTION_NO_BIDI;
        float usize = this.fontSize;
        final Phrase phrase = composePhrase(ptext, ufont, fcolor, usize);
        if ((this.options & MULTILINE) != 0) {
            final float width = this.box.getWidth() - 4 * offsetX - this.extraMarginLeft;
            final float factor = ufont.getFontDescriptor(BaseFont.BBOXURY, 1) - ufont.getFontDescriptor(BaseFont.BBOXLLY, 1);
            final ColumnText ct = new ColumnText(null);
            if (usize == 0) {
                usize = h / factor;
                if (usize > 4) {
                    if (usize > 12) {
						usize = 12;
					}
                    final float step = Math.max((usize - 4) / 10, 0.2f);
                    ct.setSimpleColumn(0, -h, width, 0);
                    ct.setAlignment(this.alignment);
                    ct.setRunDirection(rtl);
                    for (; usize > 4; usize -= step) {
                        ct.setYLine(0);
                        changeFontSize(phrase, usize);
                        ct.setText(phrase);
                        ct.setLeading(factor * usize);
                        final int status = ct.go(true);
                        if ((status & ColumnText.NO_MORE_COLUMN) == 0) {
							break;
						}
                    }
                }
                if (usize < 4) {
					usize = 4;
				}
            }
            changeFontSize(phrase, usize);
            ct.setCanvas(app);
            final float leading = usize * factor;
            final float offsetY = offsetX + h - ufont.getFontDescriptor(BaseFont.BBOXURY, usize);
            ct.setSimpleColumn(this.extraMarginLeft + 2 * offsetX, -20000, this.box.getWidth() - 2 * offsetX, offsetY + leading);
            ct.setLeading(leading);
            ct.setAlignment(this.alignment);
            ct.setRunDirection(rtl);
            ct.setText(phrase);
            ct.go();
        }
        else {
            if (usize == 0) {
                final float maxCalculatedSize = h / (ufont.getFontDescriptor(BaseFont.BBOXURX, 1) - ufont.getFontDescriptor(BaseFont.BBOXLLY, 1));
                changeFontSize(phrase, 1);
                final float wd = ColumnText.getWidth(phrase, rtl, 0);
                if (wd == 0) {
					usize = maxCalculatedSize;
				} else {
					usize = Math.min(maxCalculatedSize, (this.box.getWidth() - this.extraMarginLeft - 4 * offsetX) / wd);
				}
                if (usize < 4) {
					usize = 4;
				}
            }
            changeFontSize(phrase, usize);
            float offsetY = offX + (this.box.getHeight() - 2*offX - ufont.getFontDescriptor(BaseFont.ASCENT, usize)) / 2;
            if (offsetY < offX) {
				offsetY = offX;
			}
            if (offsetY - offX < -ufont.getFontDescriptor(BaseFont.DESCENT, usize)) {
                final float ny = -ufont.getFontDescriptor(BaseFont.DESCENT, usize) + offX;
                final float dy = this.box.getHeight() - offX - ufont.getFontDescriptor(BaseFont.ASCENT, usize);
                offsetY = Math.min(ny, Math.max(offsetY, dy));
            }
            if ((this.options & COMB) != 0 && this.maxCharacterLength > 0) {
                final int textLen = Math.min(this.maxCharacterLength, ptext.length());
                int position = 0;
                if (this.alignment == Element.ALIGN_RIGHT) {
					position = this.maxCharacterLength - textLen;
				} else if (this.alignment == Element.ALIGN_CENTER) {
					position = (this.maxCharacterLength - textLen) / 2;
				}
                final float step = (this.box.getWidth() - this.extraMarginLeft) / this.maxCharacterLength;
                float start = step / 2 + position * step;
                if (this.textColor == null) {
					app.setGrayFill(0);
				} else {
					app.setColorFill(this.textColor);
				}
                app.beginText();
                for (int k = 0; k < phrase.size(); ++k) {
                    final Chunk ck = (Chunk)phrase.get(k);
                    final BaseFont bf = ck.getFont().getBaseFont();
                    app.setFontAndSize(bf, usize);
                    final StringBuffer sb = ck.append("");
                    for (int j = 0; j < sb.length(); ++j) {
                        final String c = sb.substring(j, j + 1);
                        final float wd = bf.getWidthPoint(c, usize);
                        app.setTextMatrix(this.extraMarginLeft + start - wd / 2, offsetY - this.extraMarginTop);
                        app.showText(c);
                        start += step;
                    }
                }
                app.endText();
            }
            else {
            	float x;
            	switch (this.alignment) {
            	case Element.ALIGN_RIGHT:
            		x = this.extraMarginLeft + this.box.getWidth() - 2 * offsetX;
            		break;
            	case Element.ALIGN_CENTER:
            		x = this.extraMarginLeft + this.box.getWidth() / 2;
            		break;
            	default:
            		x = this.extraMarginLeft + 2 * offsetX;
            	}
            	ColumnText.showTextAligned(app, this.alignment, phrase, x, offsetY - this.extraMarginTop, 0, rtl, 0);
            }
        }
        app.restoreState();
        app.endVariableText();
        return app;
    }

    /**
     * Get the <code>PdfAppearance</code> of a list field
     * @throws IOException on error
     * @throws DocumentException on error
     * @return A <code>PdfAppearance</code>
     */
    PdfAppearance getListAppearance() throws IOException, DocumentException {
        final PdfAppearance app = getBorderAppearance();
        app.beginVariableText();
        if (this.choices == null || this.choices.length == 0) {
            app.endVariableText();
            return app;
        }
        int topChoice = this.choiceSelection;
        if (topChoice >= this.choices.length) {
			topChoice = this.choices.length - 1;
		}
        if (topChoice < 0) {
			topChoice = 0;
		}
        final BaseFont ufont = getRealFont();
        float usize = this.fontSize;
        if (usize == 0) {
			usize = 12;
		}
        final boolean borderExtra = this.borderStyle == PdfBorderDictionary.STYLE_BEVELED || this.borderStyle == PdfBorderDictionary.STYLE_INSET;
        float h = this.box.getHeight() - this.borderWidth * 2;
        float offsetX = this.borderWidth;
        if (borderExtra) {
            h -= this.borderWidth * 2;
            offsetX *= 2;
        }
        final float leading = ufont.getFontDescriptor(BaseFont.BBOXURY, usize) - ufont.getFontDescriptor(BaseFont.BBOXLLY, usize);
        final int maxFit = (int)(h / leading) + 1;
        int first = 0;
        int last = 0;
        last = topChoice + maxFit / 2 + 1;
        first = last - maxFit;
        if (first < 0) {
            last += first;
            first = 0;
        }
//        first = topChoice;
        last = first + maxFit;
        if (last > this.choices.length) {
			last = this.choices.length;
		}
        this.topFirst = first;
        app.saveState();
        app.rectangle(offsetX, offsetX, this.box.getWidth() - 2 * offsetX, this.box.getHeight() - 2 * offsetX);
        app.clip();
        app.newPath();
        final Color fcolor = this.textColor == null ? GrayColor.GRAYBLACK : this.textColor;
        app.setColorFill(new Color(10, 36, 106));
        app.rectangle(offsetX, offsetX + h - (topChoice - first + 1) * leading, this.box.getWidth() - 2 * offsetX, leading);
        app.fill();
        final float xp = offsetX * 2;
        float yp = offsetX + h - ufont.getFontDescriptor(BaseFont.BBOXURY, usize);
        for (int idx = first; idx < last; ++idx, yp -= leading) {
            String ptext = this.choices[idx];
            final int rtl = checkRTL(ptext) ? PdfWriter.RUN_DIRECTION_LTR : PdfWriter.RUN_DIRECTION_NO_BIDI;
            ptext = removeCRLF(ptext);
            final Phrase phrase = composePhrase(ptext, ufont, idx == topChoice ? GrayColor.GRAYWHITE : fcolor, usize);
            ColumnText.showTextAligned(app, Element.ALIGN_LEFT, phrase, xp, yp, 0, rtl, 0);
        }
        app.restoreState();
        app.endVariableText();
        return app;
    }

    /**
     * Gets a new text field.
     * @throws IOException on error
     * @throws DocumentException on error
     * @return a new text field
     */
    public PdfFormField getTextField() throws IOException, DocumentException {
        if (this.maxCharacterLength <= 0) {
			this.options &= ~COMB;
		}
        if ((this.options & COMB) != 0) {
			this.options &= ~MULTILINE;
		}
        final PdfFormField field = PdfFormField.createTextField(this.writer, false, false, this.maxCharacterLength);
        field.setWidget(this.box, PdfAnnotation.HIGHLIGHT_INVERT);
        switch (this.alignment) {
            case Element.ALIGN_CENTER:
                field.setQuadding(PdfFormField.Q_CENTER);
                break;
            case Element.ALIGN_RIGHT:
                field.setQuadding(PdfFormField.Q_RIGHT);
                break;
        }
        if (this.rotation != 0) {
			field.setMKRotation(this.rotation);
		}
        if (this.fieldName != null) {
            field.setFieldName(this.fieldName);
            if (!"".equals(this.text)) {
				field.setValueAsString(this.text);
			}
            if (this.defaultText != null) {
				field.setDefaultValueAsString(this.defaultText);
			}
            if ((this.options & READ_ONLY) != 0) {
				field.setFieldFlags(PdfFormField.FF_READ_ONLY);
			}
            if ((this.options & REQUIRED) != 0) {
				field.setFieldFlags(PdfFormField.FF_REQUIRED);
			}
            if ((this.options & MULTILINE) != 0) {
				field.setFieldFlags(PdfFormField.FF_MULTILINE);
			}
            if ((this.options & DO_NOT_SCROLL) != 0) {
				field.setFieldFlags(PdfFormField.FF_DONOTSCROLL);
			}
            if ((this.options & PASSWORD) != 0) {
				field.setFieldFlags(PdfFormField.FF_PASSWORD);
			}
            if ((this.options & FILE_SELECTION) != 0) {
				field.setFieldFlags(PdfFormField.FF_FILESELECT);
			}
            if ((this.options & DO_NOT_SPELL_CHECK) != 0) {
				field.setFieldFlags(PdfFormField.FF_DONOTSPELLCHECK);
			}
            if ((this.options & COMB) != 0) {
				field.setFieldFlags(PdfFormField.FF_COMB);
			}
        }
        field.setBorderStyle(new PdfBorderDictionary(this.borderWidth, this.borderStyle, new PdfDashPattern(3)));
        final PdfAppearance tp = getAppearance();
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tp);
        final PdfAppearance da = (PdfAppearance)tp.getDuplicate();
        da.setFontAndSize(getRealFont(), this.fontSize);
        if (this.textColor == null) {
			da.setGrayFill(0);
		} else {
			da.setColorFill(this.textColor);
		}
        field.setDefaultAppearanceString(da);
        if (this.borderColor != null) {
			field.setMKBorderColor(this.borderColor);
		}
        if (this.backgroundColor != null) {
			field.setMKBackgroundColor(this.backgroundColor);
		}
        switch (this.visibility) {
            case HIDDEN:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_HIDDEN);
                break;
            case VISIBLE_BUT_DOES_NOT_PRINT:
                break;
            case HIDDEN_BUT_PRINTABLE:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_NOVIEW);
                break;
            default:
                field.setFlags(PdfAnnotation.FLAGS_PRINT);
                break;
        }
        return field;
    }

    /**
     * Gets a new combo field.
     * @throws IOException on error
     * @throws DocumentException on error
     * @return a new combo field
     */
    public PdfFormField getComboField() throws IOException, DocumentException {
        return getChoiceField(false);
    }

    /**
     * Gets a new list field.
     * @throws IOException on error
     * @throws DocumentException on error
     * @return a new list field
     */
    public PdfFormField getListField() throws IOException, DocumentException {
        return getChoiceField(true);
    }

    protected PdfFormField getChoiceField(final boolean isList) throws IOException, DocumentException {
        this.options &= ~MULTILINE & ~COMB;
        String uchoices[] = this.choices;
        if (uchoices == null) {
			uchoices = new String[0];
		}
        int topChoice = this.choiceSelection;
        if (topChoice >= uchoices.length) {
			topChoice = uchoices.length - 1;
		}
        if (this.text == null)
		 {
			this.text = ""; //fixed by Kazuya Ujihara (ujihara.jp)
		}
        if (topChoice >= 0) {
			this.text = uchoices[topChoice];
		}
        if (topChoice < 0) {
			topChoice = 0;
		}
        PdfFormField field = null;
        String mix[][] = null;
        if (this.choiceExports == null) {
            if (isList) {
				field = PdfFormField.createList(this.writer, uchoices, topChoice);
			} else {
				field = PdfFormField.createCombo(this.writer, (this.options & EDIT) != 0, uchoices, topChoice);
			}
        }
        else {
            mix = new String[uchoices.length][2];
            for (int k = 0; k < mix.length; ++k) {
				mix[k][0] = mix[k][1] = uchoices[k];
			}
            final int top = Math.min(uchoices.length, this.choiceExports.length);
            for (int k = 0; k < top; ++k) {
                if (this.choiceExports[k] != null) {
					mix[k][0] = this.choiceExports[k];
				}
            }
            if (isList) {
				field = PdfFormField.createList(this.writer, mix, topChoice);
			} else {
				field = PdfFormField.createCombo(this.writer, (this.options & EDIT) != 0, mix, topChoice);
			}
        }
        field.setWidget(this.box, PdfAnnotation.HIGHLIGHT_INVERT);
        if (this.rotation != 0) {
			field.setMKRotation(this.rotation);
		}
        if (this.fieldName != null) {
            field.setFieldName(this.fieldName);
            if (uchoices.length > 0) {
                if (mix != null) {
                    field.setValueAsString(mix[topChoice][0]);
                    field.setDefaultValueAsString(mix[topChoice][0]);
                }
                else {
                    field.setValueAsString(this.text);
                    field.setDefaultValueAsString(this.text);
                }
            }
            if ((this.options & READ_ONLY) != 0) {
				field.setFieldFlags(PdfFormField.FF_READ_ONLY);
			}
            if ((this.options & REQUIRED) != 0) {
				field.setFieldFlags(PdfFormField.FF_REQUIRED);
			}
            if ((this.options & DO_NOT_SPELL_CHECK) != 0) {
				field.setFieldFlags(PdfFormField.FF_DONOTSPELLCHECK);
			}
        }
        field.setBorderStyle(new PdfBorderDictionary(this.borderWidth, this.borderStyle, new PdfDashPattern(3)));
        PdfAppearance tp;
        if (isList) {
            tp = getListAppearance();
            if (this.topFirst > 0) {
				field.put(PdfName.TI, new PdfNumber(this.topFirst));
			}
        } else {
			tp = getAppearance();
		}
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tp);
        final PdfAppearance da = (PdfAppearance)tp.getDuplicate();
        da.setFontAndSize(getRealFont(), this.fontSize);
        if (this.textColor == null) {
			da.setGrayFill(0);
		} else {
			da.setColorFill(this.textColor);
		}
        field.setDefaultAppearanceString(da);
        if (this.borderColor != null) {
			field.setMKBorderColor(this.borderColor);
		}
        if (this.backgroundColor != null) {
			field.setMKBackgroundColor(this.backgroundColor);
		}
        switch (this.visibility) {
            case HIDDEN:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_HIDDEN);
                break;
            case VISIBLE_BUT_DOES_NOT_PRINT:
                break;
            case HIDDEN_BUT_PRINTABLE:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_NOVIEW);
                break;
            default:
                field.setFlags(PdfAnnotation.FLAGS_PRINT);
                break;
        }
        return field;
    }

    /**
     * Gets the default text.
     * @return the default text
     */
    public String getDefaultText() {
        return this.defaultText;
    }

    /**
     * Sets the default text. It is only meaningful for text fields.
     * @param defaultText the default text
     */
    public void setDefaultText(final String defaultText) {
        this.defaultText = defaultText;
    }

    /**
     * Gets the choices to be presented to the user in list/combo fields.
     * @return the choices to be presented to the user
     */
    public String[] getChoices() {
        return this.choices;
    }

    /**
     * Sets the choices to be presented to the user in list/combo fields.
     * @param choices the choices to be presented to the user
     */
    public void setChoices(final String[] choices) {
        this.choices = choices;
    }

    /**
     * Gets the export values in list/combo fields.
     * @return the export values in list/combo fields
     */
    public String[] getChoiceExports() {
        return this.choiceExports;
    }

    /**
     * Sets the export values in list/combo fields. If this array
     * is <CODE>null</CODE> then the choice values will also be used
     * as the export values.
     * @param choiceExports the export values in list/combo fields
     */
    public void setChoiceExports(final String[] choiceExports) {
        this.choiceExports = choiceExports;
    }

    /**
     * Gets the zero based index of the selected item.
     * @return the zero based index of the selected item
     */
    public int getChoiceSelection() {
        return this.choiceSelection;
    }

    /**
     * Sets the zero based index of the selected item.
     * @param choiceSelection the zero based index of the selected item
     */
    public void setChoiceSelection(final int choiceSelection) {
        this.choiceSelection = choiceSelection;
    }

    int getTopFirst() {
        return this.topFirst;
    }

    /**
     * Sets extra margins in text fields to better mimic the Acrobat layout.
     * @param extraMarginLeft the extra margin left
     * @param extraMarginTop the extra margin top
     */
    public void setExtraMargin(final float extraMarginLeft, final float extraMarginTop) {
        this.extraMarginLeft = extraMarginLeft;
        this.extraMarginTop = extraMarginTop;
    }

    /**
     * Holds value of property substitutionFonts.
     */
    private ArrayList substitutionFonts;

    /**
     * Gets the list of substitution fonts. The list is composed of <CODE>BaseFont</CODE> and can be <CODE>null</CODE>. The fonts in this list will be used if the original
     * font doesn't contain the needed glyphs.
     * @return the list
     */
    public ArrayList getSubstitutionFonts() {
        return this.substitutionFonts;
    }

    /**
     * Sets a list of substitution fonts. The list is composed of <CODE>BaseFont</CODE> and can also be <CODE>null</CODE>. The fonts in this list will be used if the original
     * font doesn't contain the needed glyphs.
     * @param substitutionFonts the list
     */
    public void setSubstitutionFonts(final ArrayList substitutionFonts) {
        this.substitutionFonts = substitutionFonts;
    }

    /**
     * Holds value of property extensionFont.
     */
    private BaseFont extensionFont;

    /**
     * Gets the extensionFont. This font will be searched before the
     * substitution fonts. It may be <code>null</code>.
     * @return the extensionFont
     */
    public BaseFont getExtensionFont() {
        return this.extensionFont;
    }

    /**
     * Sets the extensionFont. This font will be searched before the
     * substitution fonts. It may be <code>null</code>.
     * @param extensionFont New value of property extensionFont.
     */
    public void setExtensionFont(final BaseFont extensionFont) {
        this.extensionFont = extensionFont;
    }
}