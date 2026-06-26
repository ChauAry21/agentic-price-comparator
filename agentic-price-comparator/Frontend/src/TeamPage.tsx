import React from 'react'
import { useNavigate } from 'react-router-dom'
import logoIcon from './assets/logo.jpg'
import './AboutUs.css'
import { ThemeToggle } from './ThemeContext.tsx'

const team = [
  {
    name: 'Tyler Ingram',
    role: 'AI/ML Engineer',
    bio: 'Tyler Ingram is a computer scientist and AI engineer passionate about applying machine learning and large language models to complex problems. He has worked on projects ranging from bioinformatics research to LLM-powered automation systems, with a focus on building reliable, production-ready software.',
  },
  {
    name: 'Jamie Rollins',
    role: 'Full-Stack & AI Engineer',
    bio: 'Jamie Rollins is a full-stack software and AI engineer with experience in agile development, prompt engineering, LLM fine-tuning, and machine learning workflows. She specializes in integrating AI systems into software applications, optimizing model performance, and developing scalable, data-driven solutions.',
  },
  {
    name: 'Aryan Chauhan',
    role: 'Software Engineer & Team Lead',
    bio: 'Aryan Chauhan is a software engineer who enjoys building AI agents and backend systems, with hands-on experience in Java, Python, C/C++, and C#. His work ranges from cloud graph validation in the cybersecurity field to a VR text editor for accessibility, and leading the team behind PricePilot AI.',
  },
  {
    name: 'Mahima Shankar',
    role: 'AI/Data Science',
    bio: "Mahima Shankar is an AI and data science engineer focused on building intelligent, user-centered digital experiences. She contributes to the design and development of PricePilot's frontend, ensuring the platform is both functional and visually cohesive.",
  },
  {
    name: 'Mohamed Salama',
    role: 'Full-Stack Developer',
    bio: 'Mohamed Salama is a software developer passionate about building scalable applications and solving real-world technical problems. His experience spans full-stack development, API integration, automation, and modern web technologies, with a focus on creating reliable, efficient, and user-friendly software solutions.',
  },
]

const TeamPage: React.FC = () => {
  const navigate = useNavigate()

  return (
    <main className="about-page">
      <header className="header">
        <div className="logo" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
          <img src={logoIcon} alt="PricePilot AI Logo" />
        </div>
        <nav className="nav-links">
          <button className="nav-btn" onClick={() => navigate('/about')}>About Us</button>
          <button className="nav-btn">Contact Us</button>
          <ThemeToggle />
          <button className="nav-btn get-started" onClick={() => navigate('/login')}>
            Get Started
          </button>
        </nav>
      </header>

      <div className="about-container">
        <section className="team-section">
          <button className="back-btn" onClick={() => navigate('/about')}>← Back to About Us</button>
          <h2>Meet the Team 👥</h2>
          <p style={{ color: '#888', marginBottom: '40px', fontSize: '16px' }}>
            The people behind PricePilot AI
          </p>
          <div className="team-grid">
            {team.map(member => (
              <div className="team-card" key={member.name}>
                <div className="team-avatar">{member.name.charAt(0)}</div>
                <h4>{member.name}</h4>
                <p className="team-role">{member.role}</p>
                <p>{member.bio}</p>
              </div>
            ))}
          </div>
        </section>
      </div>
    </main>
  )
}

export default TeamPage
