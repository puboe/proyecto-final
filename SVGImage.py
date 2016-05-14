import svgwrite
from svgwrite.image import Image

class SVGImage(object):
	def __init__(self, uri, fileName):
		self.dwg = svgwrite.Drawing(fileName, profile='tiny')
		image = Image(uri, insert=(0,0), size=("100%","100%"))
		image.stretch()
		self.dwg.add(image)

	def addLine(self, v1, v2):
		self.dwg.add(self.dwg.line(v1, v2, stroke=svgwrite.rgb(0, 50, 100, '%')))

	def addLabel(self, label):
		self.dwg.add(self.dwg.text(label, insert=("5%", "95%"), fill='red'))

	def save(self):
		self.dwg.save()


# image = SVGImage("test.jpeg", "test2.svg")

# image.addLine(("5%","5%"), ("95%","5%"))
# image.addLine(("95%","5%"), ("95%", "95%"))
# image.addLine(("5%","95%"), ("95%", "95%"))
# image.addLine(("5%","5%"), ("5%", "95%"))
# image.addLabel("Aca podemos poner un label que diga la fecha por ejemplo")

# image.save()
