#!/usr/bin/env ruby

require "json"
require "faraday"
require "faraday/net_http"
Faraday.default_adapter = :net_http

def create_request_body(n = 1)
  recs = (1..3).to_a.map do |i|
    { recommendationId: i,
      author: "author #{i}",
      rate: i,
      content: "content #{i}" }
  end
  revs = (1..3).to_a.map do |i|
    { reviewId: i,
      author: "author #{i}",
      subject: "subject #{i}",
      content: "content #{i}" }
  end

  {
    productId: n,
    name: "product #{n}",
    weight: n,
    recommendations: recs,
    reviews: revs,
  }.to_json
end

conn = Faraday.new(
  url: "http://localhost:8080",
  headers: { "Content-Type" => "application/json" },
)

body = create_request_body rand(999)

puts "Create Product Composite"
puts body

response = conn.post("/product-composite") do |req|
  req.body = body
end

puts "\n"

if response.status == 200
  puts "Post Successful"
else
  puts "Post not successful: Response Code -> #{response.status}"
end

puts response.body

puts "\nDeleting the previous product"

response = conn.delete("/product-composite/#{JSON.parse(body)["productId"]}")

if response.status == 200
  puts "Delete Successful"
else
  puts "Delete not successful: Response Code -> #{response.status}"
end

puts response.body

puts "\n"
puts "Open API documentation page"

response = conn.get("/openapi/swagger-ui.html")

if response.status == 302
  puts "API documentation page opened successfully"
else
  puts "Error opening documentation page: Response Code -> #{response.status}"
end

puts response.body
